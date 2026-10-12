package com.finex.auth.agent.workflow;

import com.finex.auth.service.digitalemployee.DigitalEmployeeService;
import com.finex.auth.service.port.FinanceAgentQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
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
                for (ControlledQueryCatalog.Field field : definition.fields()) {
                    if (confirmed.containsKey(groupPrefix + field.key()) && !Set.of("companyId", "voucherStatus",
                            "supplierCode").contains(field.key()))
                        values.put(field.key(), confirmed.get(groupPrefix + field.key()));
                }
                Set<String> allowed = definition.fields().stream().map(ControlledQueryCatalog.Field::key)
                        .collect(java.util.stream.Collectors.toSet());
                if (!allowed.containsAll(values.keySet())) throw new IllegalArgumentException("工具参数不在受控目录中");
                if (allowed.contains("companyId")) {
                    Set<String> companies = grant.companyIds();
                    if (companies.isEmpty()) throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
                    List<FinanceAgentQueryPort.AccountSet> accounts = finance.accountSets(grant).stream()
                            .filter(item -> companies.contains(item.companyId()))
                            .toList();
                    String companyId = resolveCompany(question, confirmed.get(groupPrefix + "companyId"),
                            values.get("companyId"), accounts);
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
                        if (allowed.contains("yearFrom"))
                            resolvePeriod(question, confirmed.get(groupPrefix + "periodRange"), values,
                                    accounts.stream().filter(item -> companyId.equals(item.companyId()))
                                            .findFirst().orElseThrow(), missing, groupPrefix, questionPrefix);
                    }
                }
                if (allowed.contains("voucherStatus")) {
                    String status = confirmed.getOrDefault(groupPrefix + "voucherStatus", statedVoucherStatus(question));
                    if (status == null) {
                        String key = groupPrefix + "voucherStatus";
                        missing.putIfAbsent(key, new Group(key, questionPrefix + "请选择凭证范围", List.of(
                                new Choice("POSTED_ONLY", "仅已记账凭证"),
                                new Choice("INCLUDE_SAVED_UNPOSTED", "包含已保存未记账凭证"))));
                    } else if (Set.of("POSTED_ONLY", "INCLUDE_SAVED_UNPOSTED").contains(status)) {
                        values.put("voucherStatus", status);
                    } else throw new IllegalArgumentException("凭证范围无效");
                }
                for (ControlledQueryCatalog.Field field : definition.fields()) {
                    if (!field.required() || !text(values.get(field.key())).isBlank()) continue;
                    if (Set.of("companyId", "voucherStatus", "supplierCode", "yearFrom", "periodFrom",
                            "yearTo", "periodTo").contains(field.key())) continue;
                    String key = groupPrefix + field.key();
                    missing.putIfAbsent(key, new Group(key, questionPrefix + "请补充" + field.label(), List.of()));
                }
                values.replaceAll((key, value) -> validateScalar(key, value));
                parameters.put(nodeKey, Map.copyOf(values));
            }
            prepared.add(new FinWorkQueryPlanner.PlannedStep(step.workflow(), step.queryText(), Map.copyOf(parameters)));
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

    private String resolveCompany(String question, String selected, Object proposed,
                                  List<FinanceAgentQueryPort.AccountSet> accounts) {
        if (selected != null) return accounts.stream().anyMatch(item -> item.companyId().equals(selected))
                ? selected : null;
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

    private void resolvePeriod(String question, String selectedRange, Map<String, Object> values,
                               FinanceAgentQueryPort.AccountSet account, Map<String, Group> missing,
                               String groupPrefix, String questionPrefix) {
        String periodText = selectedRange == null ? question : selectedRange;
        if (selectedRange == null && question.contains("所有") && !YEAR.matcher(question).find()) {
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
        try {
            YearMonth start = YearMonth.of(Integer.parseInt(text(values.get("yearFrom"))),
                    Integer.parseInt(text(values.get("periodFrom"))));
            YearMonth end = YearMonth.of(Integer.parseInt(text(values.get("yearTo"))),
                    Integer.parseInt(text(values.get("periodTo"))));
            YearMonth earliest = YearMonth.of(account.startYear(), account.startPeriod());
            YearMonth latest = YearMonth.of(account.endYear(), account.endPeriod());
            if (start.isBefore(earliest) || end.isAfter(latest) || start.isAfter(end))
                throw new IllegalArgumentException("会计期间超出账套可查范围");
        } catch (NumberFormatException exception) { throw new IllegalArgumentException("会计期间无效"); }
    }

    private Object validateScalar(String key, Object value) {
        if (value == null || !(value instanceof String || value instanceof Number || value instanceof Boolean)
                || text(value).length() > 256) throw new IllegalArgumentException("工具参数无效: " + key);
        return value;
    }

    private String statedVoucherStatus(String question) {
        if (question.contains("不含未记账") || question.contains("不包含未记账")
                || question.contains("仅已记账") || question.contains("只看已记账")) return "POSTED_ONLY";
        if (question.matches("(?s).*(?:包含|包括|含)已保存的?未记账.*")
                || question.contains("包含未记账") || question.contains("包括未记账")
                || question.contains("含未记账")) return "INCLUDE_SAVED_UNPOSTED";
        return null;
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
}
