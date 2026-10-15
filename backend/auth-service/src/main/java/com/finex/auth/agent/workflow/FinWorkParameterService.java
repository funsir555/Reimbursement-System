package com.finex.auth.agent.workflow;

import com.finex.auth.service.digitalemployee.DigitalEmployeeService;
import com.finex.auth.service.port.FinanceAgentQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Completes typed inputs before a reviewed workflow can create a run. */
@Service
@RequiredArgsConstructor
public class FinWorkParameterService {
    private static final Pattern COMPANY_PHRASE = Pattern.compile("在(.+?)(?:的|账套|公司|所有|往来|明细|$)");
    private static final Pattern YEAR = Pattern.compile("(?:19|20)\\d{2}\\s*年");
    private static final Pattern MONTH = Pattern.compile("((?:19|20)\\d{2})\\s*年\\s*(1[0-2]|0?[1-9])\\s*月");
    private static final Pattern SAME_YEAR_END = Pattern.compile("(?:至|到|—|-)\\s*(1[0-2]|0?[1-9])\\s*月");
    private static final Pattern ALL_PERIODS = Pattern.compile("全部期间|所有期间|全期间|从建账至今|自建账至今|建账以来|全部的?往来明细|所有的?往来明细");
    private static final Pattern STATED_PERIOD = Pattern.compile("(?:19|20)\\d{2}\\s*年|(?:19|20)\\d{2}[-/]\\d{1,2}|去年|上年|今年|本年|上个月|本月|上季度|本季度|全部期间|所有期间|全期间|建账|全部的?往来明细|所有的?往来明细");
    private final QueryAuthorizationService authorizations;
    private final DigitalEmployeeService employees;
    private final FinanceAgentQueryPort finance;

    public record Choice(String id, String label) { }
    public record Group(String key, String question, List<Choice> options) { }
    public record Preparation(List<FinWorkQueryPlanner.PlannedStep> steps, List<Group> groups) { }

    public Map<Long, String> authorizationSignatures(Long userId, List<SelectedWorkflow> workflows) {
        Map<Long, String> result = new LinkedHashMap<>();
        for (SelectedWorkflow workflow : workflows) {
            if (workflow.getQueryPermissionCodes() == null) continue;
            String signature = workflow.getQueryPermissionCodes().stream().sorted()
                    .map(permission -> grant(userId, workflow, permission).digest())
                    .reduce("", (left, right) -> left + ":" + right);
            result.put(workflow.getWorkflowBindingId(), signature);
        }
        return Map.copyOf(result);
    }

    public Preparation prepare(Long userId, String question, List<FinWorkQueryPlanner.PlannedStep> steps,
                               Map<String, String> confirmed) {
        Map<String, Group> missing = new LinkedHashMap<>();
        List<FinWorkQueryPlanner.PlannedStep> prepared = new ArrayList<>();
        Map<String, QueryAuthorization> grants = new LinkedHashMap<>();
        boolean multipleTools = steps.stream().mapToInt(step -> toolNodes(step.workflow()).size()).sum() > 1;
        for (FinWorkQueryPlanner.PlannedStep step : steps) {
            SelectedWorkflow workflow = step.workflow();
            for (String permission : workflow.getQueryPermissionCodes()) {
                QueryAuthorization grant = grant(userId, workflow, permission);
                grants.put(workflow.getWorkflowBindingId() + ":" + permission, grant);
            }
        }

        for (FinWorkQueryPlanner.PlannedStep step : steps) {
            Map<String, Object> parameters = new LinkedHashMap<>(step.parameters());
            for (Map<String, Object> node : toolNodes(step.workflow())) {
                String nodeKey = String.valueOf(node.get("nodeKey"));
                ControlledQueryCatalog.Definition definition = definition(node);
                String groupPrefix = multipleTools ? step.workflow().getWorkflowBindingId() + ":" + nodeKey + ":" : "";
                String questionPrefix = multipleTools ? definition.label() + "：" : "";
                QueryAuthorization grant = grants.get(step.workflow().getWorkflowBindingId() + ":"
                        + definition.permission());
                Map<String, Object> values = parameterMap(parameters.get(nodeKey));
                // Empty model proposals are missing inputs, not executable parameter values.
                values.entrySet().removeIf(entry -> entry.getValue() == null
                        || entry.getValue() instanceof String text && text.isBlank());
                Map<String, String> evidence = step.evidence().getOrDefault(nodeKey, Map.of());
                values.entrySet().removeIf(entry -> !evidence.containsKey(entry.getKey())
                        && !question.contains(text(entry.getValue())));
                for (ControlledQueryCatalog.Field field : definition.fields()) {
                    if (confirmed.containsKey(groupPrefix + field.key()) && !Set.of("companyId", "voucherStatus",
                            "supplierCode").contains(field.key()))
                        values.put(field.key(), confirmed.get(groupPrefix + field.key()));
                }
                Set<String> allowed = definition.semanticInputs().stream().map(item -> String.valueOf(item.get("key")))
                        .collect(java.util.stream.Collectors.toSet());
                if (!allowed.containsAll(values.keySet())) throw new IllegalArgumentException("工具参数不在受控目录中");
                if (allowed.contains("companyId")) {
                    Set<String> companies = grant.companyIds();
                    if (companies.isEmpty()) throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
                    List<FinanceAgentQueryPort.AccountSet> accounts = finance.accountSets(grant).stream()
                            .filter(item -> companies.contains(item.companyId()))
                            .toList();
                    String companyId = resolveCompany(question, confirmed.get(groupPrefix + "companyId"),
                            values.get("companyId"), text(values.remove("companyName")), accounts);
                    if (companyId == null) {
                        String key = groupPrefix + "companyId";
                        missing.putIfAbsent(key, new Group(key, questionPrefix + "请选择要查询的账套",
                                accounts.stream().map(item -> new Choice(item.companyId(),
                                        item.companyName() + "（" + item.companyId() + "）")).toList()));
                    } else {
                        values.put("companyId", companyId);
                        if ("SUPPLIER_LEDGER".equals(definition.kind()))
                            resolveSupplier(question, confirmed, grant, companyId, values, missing,
                                    groupPrefix, questionPrefix);
                        if ("BALANCE".equals(definition.kind()))
                            resolveSubjectNames(grant, companyId, values, missing, groupPrefix, questionPrefix);
                        if (allowed.contains("yearFrom")) {
                            if (hasCompletePeriod(values) && !STATED_PERIOD.matcher(question).find())
                                for (String field : List.of("yearFrom", "periodFrom", "yearTo", "periodTo"))
                                    values.remove(field);
                            resolvePeriod(question, confirmed.get(groupPrefix + "periodRange"), values,
                                    accounts.stream().filter(item -> companyId.equals(item.companyId()))
                                            .findFirst().orElseThrow(), missing, groupPrefix, questionPrefix);
                        }
                    }
                }
                if (allowed.contains("voucherStatus")) {
                    if (latestVoucherUtterance(question).matches("(?s).*(?:仅|只看|只查|只要)已保存的?未记账.*"))
                        throw new IllegalArgumentException("当前工具不支持仅查询未记账凭证，请选择仅已记账或包含已保存未记账凭证");
                    String stated = statedVoucherStatus(question);
                    String proposed = text(values.get("voucherStatus"));
                    String source = evidence.getOrDefault("voucherStatus", "");
                    if (stated == null && !source.matches("(?s).*(?:记账|过账|入账|保存|草稿).*"))
                        proposed = "";
                    String status = confirmed.getOrDefault(groupPrefix + "voucherStatus",
                            stated == null ? proposed : stated);
                    if (status.isBlank()) status = null;
                    if (status == null) {
                        String key = groupPrefix + "voucherStatus";
                        missing.putIfAbsent(key, new Group(key, questionPrefix + "请选择凭证范围", List.of(
                                new Choice("POSTED_ONLY", "仅已记账凭证"),
                                new Choice("INCLUDE_SAVED_UNPOSTED", "包含已保存未记账凭证"))));
                    } else if (Set.of("POSTED_ONLY", "INCLUDE_SAVED_UNPOSTED").contains(status)) {
                        values.put("voucherStatus", status);
                    } else {
                        values.remove("voucherStatus");
                        String key = groupPrefix + "voucherStatus";
                        missing.putIfAbsent(key, new Group(key, questionPrefix + "请确认凭证范围", List.of(
                                new Choice("POSTED_ONLY", "仅已记账凭证"),
                                new Choice("INCLUDE_SAVED_UNPOSTED", "包含已保存未记账凭证"))));
                    }
                }
                if ("CLOSE".equals(definition.kind()))
                    resolveClosePeriod(question, values, missing, groupPrefix, questionPrefix);
                if ("EXPENSE".equals(definition.kind())) {
                    validateExpense(question, values);
                    String dateQuestion = latestExpenseDateUtterance(question);
                    if (dateQuestion.matches("(?s).*(?:(?:19|20)\\d{2}[-/年]\\d{1,2}|昨天|今天|上周|本周|最近\\d+天).*")
                            && text(values.get("startDate")).isBlank() && text(values.get("endDate")).isBlank()) {
                        String key = groupPrefix + "expenseDateRange";
                        missing.putIfAbsent(key, new Group(key,
                                questionPrefix + "请明确报销日期范围，例如 2026-09-01 至 2026-09-30", List.of()));
                    }
                    String amountQuestion = latestAmountUtterance(question);
                    boolean strictAmount = amountQuestion.matches(
                            "(?s).*(?<!不)(?:超过|大于|低于|小于|少于)\\s*\\d+.*");
                    if (amountQuestion.matches("(?s).*(?:不少于|不超过|金额在|金额为|金额等于|超过|大于|低于|小于|少于).*")
                            && !strictAmount
                            && !amountQuestion.matches("(?s).*(?:不限金额|不限制金额|不看金额).*")
                            && text(values.get("minAmount")).isBlank() && text(values.get("maxAmount")).isBlank()) {
                        String key = groupPrefix + "expenseAmountRange";
                        missing.putIfAbsent(key, new Group(key,
                                questionPrefix + "请明确报销金额下限或上限", List.of()));
                    }
                    if (strictAmount) {
                        values.remove("minAmount");
                        values.remove("maxAmount");
                        String key = groupPrefix + "amountBoundary";
                        missing.putIfAbsent(key, new Group(key,
                                questionPrefix + "当前金额筛选包含边界，请改说“不少于”或“不超过”具体金额",
                                List.of()));
                    }
                }
                if ("BALANCE".equals(definition.kind())) validateBalanceOptions(values);
                if (allowed.contains("page")) validatePage(values, "page", 1, Integer.MAX_VALUE);
                if (allowed.contains("pageSize")) validatePage(values, "pageSize", 1, 200);
                if (allowed.contains("page") && allowed.contains("pageSize")) {
                    long page = text(values.get("page")).isBlank() ? 1
                            : Long.parseLong(text(values.get("page")));
                    long size = text(values.get("pageSize")).isBlank() ? 50
                            : Long.parseLong(text(values.get("pageSize")));
                    if ((page - 1) * size > Integer.MAX_VALUE)
                        throw new IllegalArgumentException("分页范围无效");
                }
                for (ControlledQueryCatalog.Field field : definition.fields()) {
                    if (!field.required() || !text(values.get(field.key())).isBlank()) continue;
                    if (Set.of("companyId", "voucherStatus", "supplierCode", "yearFrom", "periodFrom",
                            "yearTo", "periodTo", "year", "period").contains(field.key())) continue;
                    String key = groupPrefix + field.key();
                    missing.putIfAbsent(key, new Group(key, questionPrefix + "请补充" + field.label(), List.of()));
                }
                values.replaceAll((key, value) -> validateScalar(key, value));
                parameters.put(nodeKey, Map.copyOf(values));
            }
            prepared.add(new FinWorkQueryPlanner.PlannedStep(step.workflow(), step.queryText(),
                    Map.copyOf(parameters), step.evidence()));
        }
        return new Preparation(List.copyOf(prepared), List.copyOf(missing.values()));
    }

    private QueryAuthorization grant(Long userId, SelectedWorkflow workflow, String permission) {
        List<String> limit = employees.companyLimit(workflow.getDigitalEmployeeId(), permission);
        return authorizations.resolve(userId, workflow.getResponsibleUserId(), permission,
                limit.isEmpty() ? null : Set.copyOf(limit));
    }

    private List<Map<String, Object>> toolNodes(SelectedWorkflow workflow) {
        Object graph = workflow.getConfig().get("workflow");
        Map<String, Object> value = graph instanceof Map<?, ?> map ? stringMap(map) : Map.of();
        return WorkflowGraphPlan.from(value).requireControlledQueryPlan().orderedNodes().stream()
                .filter(node -> "tool".equalsIgnoreCase(String.valueOf(node.get("nodeType")))).toList();
    }

    private ControlledQueryCatalog.Definition definition(Map<String, Object> node) {
        return ControlledQueryCatalog.require(String.valueOf(WorkflowGraphPlan.nodeConfig(node).get("toolCode")));
    }

    private Map<String, Object> parameterMap(Object source) {
        if (!(source instanceof Map<?, ?> map)) return new LinkedHashMap<>();
        return new LinkedHashMap<>(stringMap(map));
    }

    private Map<String, Object> stringMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private String resolveCompany(String question, String selected, Object proposed, String proposedName,
                                  List<FinanceAgentQueryPort.AccountSet> accounts) {
        if (selected != null) return accounts.stream().anyMatch(item -> item.companyId().equals(selected))
                ? selected : null;
        if (!proposedName.isBlank()) {
            List<FinanceAgentQueryPort.AccountSet> byName = accounts.stream()
                    .filter(item -> item.companyName().equals(proposedName)
                            || item.companyName().contains(proposedName)).toList();
            if (byName.size() == 1) return byName.get(0).companyId();
            return null;
        }
        List<FinanceAgentQueryPort.AccountSet> named = accounts.stream().filter(item ->
                question.contains(item.companyId()) || question.contains(item.companyName())).toList();
        if (named.size() == 1) return named.get(0).companyId();
        if (named.size() > 1) return null;
        Matcher phrase = COMPANY_PHRASE.matcher(question);
        if (phrase.find() && phrase.group(1).trim().length() >= 2) {
            String requested = phrase.group(1).trim();
            List<FinanceAgentQueryPort.AccountSet> partial = accounts.stream()
                    .filter(item -> item.companyName().contains(requested)).toList();
            if (partial.size() == 1) return partial.get(0).companyId();
            if (partial.size() > 1) return null;
            return null;
        }
        String proposedId = text(proposed);
        if (!proposedId.isBlank() && accounts.stream().anyMatch(item -> item.companyId().equals(proposedId)
                && question.contains(item.companyName()))) return proposedId;
        return accounts.size() == 1 && !question.contains("公司") && !question.contains("账套")
                ? accounts.get(0).companyId() : null;
    }

    private void resolveSupplier(String question, Map<String, String> confirmed, QueryAuthorization grant,
                                 String companyId, Map<String, Object> values, Map<String, Group> missing,
                                 String groupPrefix, String questionPrefix) {
        SupplierLedgerSelection.Match match = SupplierLedgerSelection.resolve(finance, grant, companyId,
                question, text(values.get("supplierName")), confirmed.get(groupPrefix + "supplierCode"));
        if (match.resolved()) {
            values.put("supplierCode", match.code());
            values.remove("supplierName");
            return;
        }
        values.remove("supplierCode");
        values.remove("supplierName");
        String key = groupPrefix + "supplierCode";
        missing.putIfAbsent(key, new Group(key,
                questionPrefix + (match.options().isEmpty() ? "请确认供应商名称" : "请选择要查询的供应商"),
                match.options().stream().map(item -> new Choice(item.code(), item.name())).toList()));
    }

    private void resolveSubjectNames(QueryAuthorization grant, String companyId,
                                     Map<String, Object> values, Map<String, Group> missing,
                                     String groupPrefix, String questionPrefix) {
        for (String side : List.of("From", "To")) {
            String name = text(values.remove("accountName" + side));
            String codeKey = "accountCode" + side;
            String code = text(values.get(codeKey));
            if (name.isBlank() && code.isBlank()) continue;
            List<FinanceAgentQueryPort.Subject> matches = finance.subjects(grant, companyId,
                    name.isBlank() ? code : name);
            List<FinanceAgentQueryPort.Subject> exact = matches.stream().filter(item ->
                    name.isBlank() ? code.equals(item.code())
                            : name.equals(item.name()) || name.equals(item.code())).toList();
            List<FinanceAgentQueryPort.Subject> usable = exact.isEmpty() ? matches : exact;
            if (usable.size() == 1) values.put(codeKey, usable.get(0).code());
            else {
                values.remove(codeKey);
                String key = groupPrefix + codeKey;
                missing.putIfAbsent(key, new Group(key, questionPrefix + "请确认" + ("From".equals(side)
                        ? "起始" : "结束") + "科目", usable.stream()
                        .map(item -> new Choice(item.code(), item.name() + "（" + item.code() + "）")).toList()));
            }
        }
    }

    private void resolveClosePeriod(String question, Map<String, Object> values,
                                    Map<String, Group> missing, String prefix, String label) {
        YearMonth[] relative = relativeMonths(latestPeriodText(question));
        if (relative != null && relative[0].equals(relative[1])) {
            values.put("year", relative[0].getYear());
            values.put("period", relative[0].getMonthValue());
        } else if (relative != null) {
            values.remove("year");
            values.remove("period");
        }
        Matcher month = MONTH.matcher(latestPeriodText(question));
        if (text(values.get("year")).isBlank() && month.find())
            values.put("year", Integer.parseInt(month.group(1)));
        if (text(values.get("period")).isBlank() && month.find(0))
            values.put("period", Integer.parseInt(month.group(2)));
        try {
            int year = Integer.parseInt(text(values.get("year")));
            int period = Integer.parseInt(text(values.get("period")));
            if (year < 1990 || year > 2090 || period < 1 || period > 12)
                throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            values.remove("year");
            values.remove("period");
            String key = prefix + "closePeriod";
            missing.putIfAbsent(key, new Group(key, label + "请说明要统计的会计年月", List.of()));
        }
    }

    private void validateExpense(String question, Map<String, Object> values) {
        YearMonth[] relative = relativeMonths(latestPeriodText(question));
        if (relative != null) {
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
            values.put("startDate", relative[0].atDay(1).toString());
            LocalDate end = relative[1].atEndOfMonth();
            values.put("endDate", end.isAfter(today) ? today.toString() : end.toString());
        }
        String scopeText = latestScopeText(question);
        if (scopeText.matches("(?s).*(?:待我审批|待我处理|需要我处理).*"))
            values.put("scope", "ASSIGNED_TO_ME");
        else if (scopeText.matches("(?s).*(?:有权查看的全部|全部有权查看|我能看的全部).*"))
            values.put("scope", "VISIBLE");
        else if (scopeText.matches("(?s).*(?:我提交的|我发起的|我申请的|我的报销单).*"))
            values.put("scope", "SUBMITTED_BY_ME");
        String scope = text(values.get("scope"));
        if (!scope.isBlank() && !Set.of("SUBMITTED_BY_ME", "ASSIGNED_TO_ME", "VISIBLE").contains(scope))
            throw new IllegalArgumentException("报销查询范围无效");
        String status = text(values.get("status"));
        if (!status.isBlank() && !Set.of("DRAFT", "PENDING_APPROVAL", "APPROVED", "REJECTED",
                "PENDING_PAYMENT", "PAYING", "PAYMENT_COMPLETED", "PAYMENT_FINISHED",
                "EXCEPTION", "PAYMENT_EXCEPTION").contains(status))
            throw new IllegalArgumentException("报销单状态无效");
        if (text(values.get("keyword")).length() > 200) throw new IllegalArgumentException("报销关键词过长");
        LocalDate start = date(values.get("startDate"));
        LocalDate end = date(values.get("endDate"));
        if (start != null && end != null && start.isAfter(end))
            throw new IllegalArgumentException("报销日期范围无效");
        BigDecimal min = amount(values.get("minAmount"));
        BigDecimal max = amount(values.get("maxAmount"));
        if (min != null && max != null && min.compareTo(max) > 0)
            throw new IllegalArgumentException("报销金额范围无效");
        validatePage(values, "limit", 1, 50);
    }

    private LocalDate date(Object value) {
        if (text(value).isBlank()) return null;
        try { return LocalDate.parse(text(value)); }
        catch (DateTimeParseException exception) { throw new IllegalArgumentException("报销日期无效"); }
    }

    private BigDecimal amount(Object value) {
        if (text(value).isBlank()) return null;
        try {
            BigDecimal number = new BigDecimal(text(value));
            if (number.signum() < 0 || number.scale() > 2) throw new NumberFormatException();
            return number;
        } catch (NumberFormatException exception) { throw new IllegalArgumentException("报销金额无效"); }
    }

    private String latestAmountUtterance(String question) {
        int supplement = question.lastIndexOf("\n补充说明：");
        if (supplement >= 0) {
            String latest = question.substring(supplement + "\n补充说明：".length());
            if (latest.matches("(?s).*(?:金额|元|不少于|不超过|超过|大于|小于|低于|少于).*"))
                return latest;
        }
        return question;
    }

    private String latestExpenseDateUtterance(String question) {
        int supplement = question.lastIndexOf("\n补充说明：");
        if (supplement >= 0) {
            String latest = question.substring(supplement + "\n补充说明：".length());
            if (latest.matches("(?s).*(?:日期|时间|昨天|今天|周|天|月|年).*")) return latest;
        }
        return question;
    }

    private String latestPeriodText(String question) {
        int supplement = question.lastIndexOf("\n补充说明：");
        if (supplement >= 0) {
            String latest = question.substring(supplement + "\n补充说明：".length());
            if (STATED_PERIOD.matcher(latest).find()) return latest;
        }
        return question;
    }

    private String latestScopeText(String question) {
        int supplement = question.lastIndexOf("\n补充说明：");
        if (supplement >= 0) {
            String latest = question.substring(supplement + "\n补充说明：".length());
            if (latest.matches("(?s).*(?:待我|需要我|我提交|我发起|我申请|我的报销单|有权查看|我能看).*"))
                return latest;
        }
        return question;
    }

    private YearMonth[] relativeMonths(String phrase) {
        YearMonth now = YearMonth.now(ZoneId.of("Asia/Shanghai"));
        if (phrase.contains("上个月")) return new YearMonth[]{now.minusMonths(1), now.minusMonths(1)};
        if (phrase.contains("本月")) return new YearMonth[]{now, now};
        if (phrase.contains("上季度") || phrase.contains("本季度")) {
            YearMonth anchor = phrase.contains("上季度") ? now.minusMonths(3) : now;
            int first = ((anchor.getMonthValue() - 1) / 3) * 3 + 1;
            YearMonth start = YearMonth.of(anchor.getYear(), first);
            return new YearMonth[]{start, start.plusMonths(2)};
        }
        Matcher lastYearQuarter = Pattern.compile("去年\\s*([一二三四1-4])季度").matcher(phrase);
        if (lastYearQuarter.find()) {
            int quarter = switch (lastYearQuarter.group(1)) {
                case "一", "1" -> 1;
                case "二", "2" -> 2;
                case "三", "3" -> 3;
                default -> 4;
            };
            YearMonth start = YearMonth.of(now.getYear() - 1, (quarter - 1) * 3 + 1);
            return new YearMonth[]{start, start.plusMonths(2)};
        }
        if (phrase.contains("去年") || phrase.contains("上年"))
            return new YearMonth[]{YearMonth.of(now.getYear() - 1, 1), YearMonth.of(now.getYear() - 1, 12)};
        if (phrase.contains("今年") || phrase.contains("本年"))
            return new YearMonth[]{YearMonth.of(now.getYear(), 1), now};
        return null;
    }

    private void validateBalanceOptions(Map<String, Object> values) {
        String display = text(values.get("assistDisplay"));
        if (!display.isBlank() && !Set.of("NAME", "CODE", "CODE_NAME").contains(display))
            throw new IllegalArgumentException("辅助项展示方式无效");
        String levels = text(values.get("subjectLevelRange"));
        if (!levels.isBlank() && !levels.matches("1-(?:[1-9]\\d*|n)"))
            throw new IllegalArgumentException("科目级次无效");
        if (!levels.isBlank() && !"1-n".equals(levels)) {
            try { Integer.parseInt(levels.substring(2)); }
            catch (NumberFormatException exception) { throw new IllegalArgumentException("科目级次无效"); }
        }
    }

    private void validatePage(Map<String, Object> values, String key, int min, int max) {
        if (text(values.get(key)).isBlank()) return;
        try {
            int page = Integer.parseInt(text(values.get(key)));
            if (page < min || page > max) throw new NumberFormatException();
        } catch (NumberFormatException exception) { throw new IllegalArgumentException(key + " 无效"); }
    }

    private void resolvePeriod(String question, String selectedRange, Map<String, Object> values,
                               FinanceAgentQueryPort.AccountSet account, Map<String, Group> missing,
                               String groupPrefix, String questionPrefix) {
        String periodText = selectedRange == null ? latestPeriodText(question) : selectedRange;
        YearMonth[] relative = relativeMonths(periodText);
        if (relative != null) {
            YearMonth latest = YearMonth.of(account.endYear(), account.endPeriod());
            if ((periodText.contains("今年") || periodText.contains("本年")
                    || periodText.contains("本季度")) && relative[1].isAfter(latest)
                    && !relative[0].isAfter(latest)) relative[1] = latest;
            values.put("yearFrom", relative[0].getYear());
            values.put("periodFrom", relative[0].getMonthValue());
            values.put("yearTo", relative[1].getYear());
            values.put("periodTo", relative[1].getMonthValue());
            validatePeriod(values, account);
            return;
        }
        if (hasCompletePeriod(values)) {
            validatePeriod(values, account);
            return;
        }
        boolean all = ALL_PERIODS.matcher(periodText).find()
                && !YEAR.matcher(periodText).find();
        if (all) {
            values.put("yearFrom", account.startYear());
            values.put("periodFrom", account.startPeriod());
            values.put("yearTo", account.endYear());
            values.put("periodTo", account.endPeriod());
            return;
        }
        Matcher selected = MONTH.matcher(periodText);
        if (selected.find()) {
            int startYear = Integer.parseInt(selected.group(1));
            int startMonth = Integer.parseInt(selected.group(2));
            int firstEnd = selected.end();
            values.put("yearFrom", startYear);
            values.put("periodFrom", startMonth);
            if (selected.find()) {
                values.put("yearTo", Integer.parseInt(selected.group(1)));
                values.put("periodTo", Integer.parseInt(selected.group(2)));
            } else {
                Matcher end = SAME_YEAR_END.matcher(periodText.substring(firstEnd));
                values.put("yearTo", startYear);
                values.put("periodTo", end.find() ? Integer.parseInt(end.group(1)) : startMonth);
            }
        } else {
            Matcher year = YEAR.matcher(periodText);
            if (year.find() && periodText.contains("全年")) {
                int selectedYear = Integer.parseInt(year.group().replace("年", "").trim());
                values.put("yearFrom", selectedYear);
                values.put("periodFrom", 1);
                values.put("yearTo", selectedYear);
                values.put("periodTo", 12);
            } else {
                for (String key : List.of("yearFrom", "periodFrom", "yearTo", "periodTo")) values.remove(key);
            }
        }
        for (String key : List.of("yearFrom", "periodFrom", "yearTo", "periodTo")) {
            if (text(values.get(key)).isBlank()) {
                String groupKey = groupPrefix + "periodRange";
                missing.putIfAbsent(groupKey, new Group(groupKey,
                        questionPrefix + "请说明会计期间，例如 2022 年 12 月或 2022 年 1 月至 12 月", List.of()));
                return;
            }
        }
        validatePeriod(values, account);
    }

    private boolean hasCompletePeriod(Map<String, Object> values) {
        return List.of("yearFrom", "periodFrom", "yearTo", "periodTo").stream()
                .allMatch(key -> !text(values.get(key)).isBlank());
    }

    private void validatePeriod(Map<String, Object> values, FinanceAgentQueryPort.AccountSet account) {
        try {
            YearMonth start = YearMonth.of(Integer.parseInt(text(values.get("yearFrom"))),
                    Integer.parseInt(text(values.get("periodFrom"))));
            YearMonth end = YearMonth.of(Integer.parseInt(text(values.get("yearTo"))),
                    Integer.parseInt(text(values.get("periodTo"))));
            YearMonth earliest = YearMonth.of(account.startYear(), account.startPeriod());
            YearMonth latest = YearMonth.of(account.endYear(), account.endPeriod());
            if (start.isBefore(earliest) || end.isAfter(latest) || start.isAfter(end))
                throw new IllegalArgumentException("会计期间超出账套可查范围");
        } catch (NumberFormatException | java.time.DateTimeException exception) {
            throw new IllegalArgumentException("会计期间无效");
        }
    }

    private Object validateScalar(String key, Object value) {
        if (value == null || !(value instanceof String || value instanceof Number || value instanceof Boolean)
                || text(value).length() > 256) throw new IllegalArgumentException("工具参数无效: " + key);
        return value;
    }

    private String statedVoucherStatus(String question) {
        return voucherIn(latestVoucherUtterance(question));
    }

    private String latestVoucherUtterance(String question) {
        int supplement = question.lastIndexOf("\n补充说明：");
        if (supplement >= 0) {
            String latest = question.substring(supplement + "\n补充说明：".length());
            if (latest.matches("(?s).*(?:凭证|记账|过账|入账|保存|草稿).*")) return latest;
        }
        return question;
    }

    public String detectVoucherStatus(String question) {
        return question == null ? null : statedVoucherStatus(question);
    }

    private String voucherIn(String question) {
        if (question.matches("(?s).*不(?:含|包含|包括)已保存的?未记账.*")
                || question.contains("不含未记账") || question.contains("不包含未记账")
                || question.contains("仅已记账") || question.contains("只看已记账")) return "POSTED_ONLY";
        if (question.matches("(?s).*(?:包含|包括|含)已保存的?未记账.*")
                || question.contains("包含未记账") || question.contains("包括未记账")
                || question.contains("含未记账")) return "INCLUDE_SAVED_UNPOSTED";
        return null;
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
}
