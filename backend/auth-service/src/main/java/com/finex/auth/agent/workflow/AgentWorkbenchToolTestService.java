package com.finex.auth.agent.workflow;

import com.finex.auth.dto.ArchiveAgentToolTestDTO;
import com.finex.auth.service.ArchiveAgentService;
import com.finex.auth.service.port.FinanceAgentQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Executes a saved, read-only workbench binding under the tester's current business scope. */
@Service
@RequiredArgsConstructor
public class AgentWorkbenchToolTestService {
    private static final Set<String> EXPENSE_PARAMETERS = Set.of(
            "scope", "keyword", "status", "startDate", "endDate", "minAmount", "maxAmount", "limit");
    private static final Set<String> FINANCE_PARAMETERS = Set.of("year", "period");

    private final ArchiveAgentService agents;
    private final QueryAuthorizationService authorizations;
    private final ToolRuntimeService runtime;
    private final FinanceAgentQueryPort finance;

    public List<FinanceAgentQueryPort.Supplier> searchSuppliers(Long userId, Long agentId,
                                                                 String companyId, String keyword) {
        var agent = agents.getAgentDetail(userId, agentId);
        if ("DISABLED".equals(agent.getStatus()) || "ARCHIVED".equals(agent.getStatus()))
            throw new IllegalStateException("当前 Agent 已停用或归档，无法测试");
        if (agent.getTools().stream().noneMatch(tool ->
                "finance.query_supplier_detail_ledger".equals(tool.get("toolCode"))
                        && !Boolean.FALSE.equals(tool.get("enabled"))))
            throw new IllegalArgumentException("请先保存供应商明细账工具绑定");
        String permission = ControlledQueryCatalog.require("finance.query_supplier_detail_ledger").permission();
        QueryAuthorization grant = authorizations.resolveForSelf(userId, permission);
        if (keyword == null || keyword.isBlank()) return List.of();
        if (keyword.length() > 128) throw new IllegalArgumentException("供应商搜索词过长");
        List<FinanceAgentQueryPort.Supplier> found = finance.suppliers(grant, companyId, keyword.trim());
        if (!grant.digest().equals(authorizations.resolveForSelf(userId, permission).digest()))
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        return found;
    }

    public Map<String, Object> test(Long userId, Long agentId, String toolCode, ArchiveAgentToolTestDTO dto) {
        ControlledQueryCatalog.Definition definition = ControlledQueryCatalog.require(toolCode);
        if (!"EXPENSE".equals(definition.kind()) && !"CLOSE".equals(definition.kind())) {
            return testRegistered(userId, agentId, definition, dto);
        }
        String permissionCode = switch (toolCode) {
            case ExpenseDocumentControlledQueryTool.TOOL_CODE -> QueryAuthorizationService.EXPENSE_QUERY;
            case FinanceUnclosedCompanyTool.TOOL_CODE -> QueryAuthorizationService.FINANCE_CLOSE_QUERY;
            default -> throw new IllegalArgumentException("该工具不支持单独测试");
        };
        var agent = agents.getAgentDetail(userId, agentId);
        if ("DISABLED".equals(agent.getStatus()) || "ARCHIVED".equals(agent.getStatus())) {
            throw new IllegalStateException("当前 Agent 已停用或归档，无法测试");
        }
        boolean bound = agent.getTools().stream().anyMatch(tool ->
                toolCode.equals(tool.get("toolCode")) && !Boolean.FALSE.equals(tool.get("enabled")));
        if (!bound) throw new IllegalArgumentException("请先保存工具绑定，再测试运行");
        Map<String, Object> parameters = dto == null || dto.getParameters() == null ? Map.of() : dto.getParameters();
        String template;
        QueryTemplateVariables variables;
        String message = "";
        if (ExpenseDocumentControlledQueryTool.TOOL_CODE.equals(toolCode)) {
            requireAllowedKeys(parameters, EXPENSE_PARAMETERS);
            template = dto == null || dto.getQueryTemplateCode() == null
                    ? ExpenseDocumentControlledQueryTool.ACCESSIBLE_QUERY_TEMPLATE_CODE : dto.getQueryTemplateCode();
            if (!runtime.supports(toolCode) || !(ExpenseDocumentControlledQueryTool.QUERY_TEMPLATE_CODE.equals(template)
                    || ExpenseDocumentControlledQueryTool.ACCESSIBLE_QUERY_TEMPLATE_CODE.equals(template))) {
                throw new IllegalArgumentException("不支持的报销查询模板");
            }
            variables = QueryTemplateVariables.builder()
                    .scope(text(parameters.get("scope")))
                    .normalizedKeyword(text(parameters.get("keyword")))
                    .status(text(parameters.get("status")))
                    .startDate(instant(parameters.get("startDate")))
                    .endDate(instant(parameters.get("endDate")))
                    .minAmount(number(parameters.get("minAmount")))
                    .maxAmount(number(parameters.get("maxAmount")))
                    .limit(integer(parameters.get("limit")))
                    .build();
        } else {
            requireAllowedKeys(parameters, FINANCE_PARAMETERS);
            template = FinanceUnclosedCompanyTool.TEMPLATE_CODE;
            if (dto != null && dto.getQueryTemplateCode() != null
                    && !template.equals(dto.getQueryTemplateCode())) {
                throw new IllegalArgumentException("不支持的结账查询模板");
            }
            int year = requiredInteger(parameters.get("year"), "会计年度");
            int period = requiredInteger(parameters.get("period"), "会计期间");
            if (year < 2000 || year > 2099 || period < 1 || period > 12) {
                throw new IllegalArgumentException("会计年度或期间无效");
            }
            message = year + " 年 " + period + " 月";
            variables = QueryTemplateVariables.builder().build();
        }
        QueryAuthorization authorization = authorizations.resolveForSelf(userId, permissionCode);
        ControlledToolResult result = runtime.execute(toolCode, ToolExecutionRequest.builder()
                .userId(userId).queryTemplateCode(template).variables(variables)
                .authorization(authorization).userMessage(message).build());
        if (!result.isSuccess()) {
            throw new IllegalArgumentException(result.getErrorMessage());
        }
        // A role or scope change during execution must not make the response readable.
        if (!authorization.digest().equals(authorizations.resolveForSelf(userId, permissionCode).digest())) {
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("toolCode", toolCode);
        response.put("queryTemplateCode", template);
        response.put("summary", result.getSummary());
        response.put("scopeDescription", "仅展示您当前有权查看的范围；单工具测试不代表已发布工作流或数字员工可用。");
        response.put("totalCount", result.getTotalCount());
        response.put("totalAmount", result.getTotalAmount());
        response.put("records", result.getRecords() == null ? List.of() : result.getRecords().stream().limit(50).toList());
        response.put("evidenceRefs", result.getEvidenceRefs() == null ? List.of() : result.getEvidenceRefs().stream().limit(50).toList());
        return response;
    }

    private Map<String, Object> testRegistered(Long userId, Long agentId,
                                               ControlledQueryCatalog.Definition definition,
                                               ArchiveAgentToolTestDTO dto) {
        var agent = agents.getAgentDetail(userId, agentId);
        if ("DISABLED".equals(agent.getStatus()) || "ARCHIVED".equals(agent.getStatus()))
            throw new IllegalStateException("当前 Agent 已停用或归档，无法测试");
        if (agent.getTools().stream().noneMatch(tool -> definition.code().equals(tool.get("toolCode"))
                && !Boolean.FALSE.equals(tool.get("enabled"))))
            throw new IllegalArgumentException("请先保存工具绑定，再测试运行");
        Map<String, Object> parameters = dto == null || dto.getParameters() == null
                ? Map.of() : dto.getParameters();
        String template = dto == null || dto.getQueryTemplateCode() == null
                ? definition.template() : dto.getQueryTemplateCode();
        if (!definition.template().equals(template)) throw new IllegalArgumentException("不支持的查询模板");
        QueryAuthorization authorization = authorizations.resolveForSelf(userId, definition.permission());
        ControlledToolResult result = runtime.execute(definition.code(), ToolExecutionRequest.builder()
                .userId(userId).queryTemplateCode(template).authorization(authorization)
                .parameters(parameters).build());
        if (!result.isSuccess()) throw new IllegalArgumentException(result.getErrorMessage());
        if (!authorization.digest().equals(authorizations.resolveForSelf(userId, definition.permission()).digest()))
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("toolCode", definition.code());
        response.put("queryTemplateCode", template);
        response.put("summary", result.getSummary());
        response.put("scopeDescription", "仅展示您当前有权查看的范围；测试结果不代表已发布版本可用。");
        response.put("totalCount", result.getTotalCount());
        response.put("records", result.getRecords());
        response.put("evidenceRefs", result.getEvidenceRefs());
        response.put("page", result.getPage());
        response.put("pageSize", result.getPageSize());
        response.put("hasMore", result.getHasMore());
        return response;
    }

    private void requireAllowedKeys(Map<String, Object> values, Set<String> allowed) {
        if (!allowed.containsAll(values.keySet())) throw new IllegalArgumentException("存在不支持的测试参数");
    }

    private String text(Object value) {
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value).trim();
    }

    private Instant instant(Object value) {
        String valueText = text(value);
        if (valueText == null) return null;
        try { return Instant.parse(valueText); }
        catch (RuntimeException exception) { throw new IllegalArgumentException("日期须使用 ISO 时间格式"); }
    }

    private Double number(Object value) {
        if (value == null || "".equals(value)) return null;
        try {
            double number = Double.parseDouble(String.valueOf(value));
            if (!Double.isFinite(number) || number < 0) throw new NumberFormatException();
            return number;
        } catch (NumberFormatException exception) { throw new IllegalArgumentException("金额须为非负数字"); }
    }

    private Integer integer(Object value) {
        if (value == null || "".equals(value)) return null;
        try { return Integer.valueOf(String.valueOf(value)); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("数量须为整数"); }
    }

    private int requiredInteger(Object value, String label) {
        Integer parsed = integer(value);
        if (parsed == null) throw new IllegalArgumentException(label + "不能为空");
        return parsed;
    }
}
