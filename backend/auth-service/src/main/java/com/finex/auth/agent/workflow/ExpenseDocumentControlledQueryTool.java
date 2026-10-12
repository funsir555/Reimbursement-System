package com.finex.auth.agent.workflow;

import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.service.port.ExpenseAgentQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 报销单受控查询工具。
 */
@Component
@RequiredArgsConstructor
public class ExpenseDocumentControlledQueryTool implements ControlledTool {

    public static final String TOOL_CODE = "expense.query_my_expenses";
    public static final String QUERY_TEMPLATE_CODE = "expense.my_expenses.v1";
    public static final String ACCESSIBLE_QUERY_TEMPLATE_CODE = "expense.accessible_expenses.v2";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "DRAFT",
            "PENDING_APPROVAL",
            "APPROVED",
            "REJECTED",
            "PENDING_PAYMENT",
            "PAYING",
            "PAYMENT_COMPLETED",
            "PAYMENT_FINISHED",
            "EXCEPTION",
            "PAYMENT_EXCEPTION"
    );

    private final ExpenseAgentQueryPort expenseQueryPort;

    @Override
    public String toolCode() {
        return TOOL_CODE;
    }

    @Override
    public boolean supportsQueryTemplate(String queryTemplateCode) {
        return QUERY_TEMPLATE_CODE.equals(queryTemplateCode)
                || ACCESSIBLE_QUERY_TEMPLATE_CODE.equals(queryTemplateCode);
    }

    @Override
    public ControlledToolResult execute(ToolExecutionRequest request) {
        if (request == null || request.getUserId() == null || request.getAuthorization() == null) {
            return failed("系统查询到您没有相关权限，请换一个问题");
        }
        Long authenticatedUserId = TenantContext.getUserId();
        if (authenticatedUserId != null && !authenticatedUserId.equals(request.getUserId())) {
            return failed("当前用户与 Agent 上下文不一致");
        }

        QueryTemplateVariables variables = request.getVariables();
        String scope = resolveScope(request.getQueryTemplateCode(), variables);
        if (scope == null) {
            return failed("不支持的报销查询范围");
        }
        String status = variables == null ? null : normalizeText(variables.getStatus());
        if (status != null && !ALLOWED_STATUSES.contains(status)) {
            return failed("不支持的报销单状态: " + status);
        }

        int limit = variables == null || variables.getLimit() == null
                ? 10
                : Math.min(Math.max(variables.getLimit(), 1), 50);
        ExpenseAgentQueryPort.Query query = new ExpenseAgentQueryPort.Query(
                        request.getUserId(),
                        null,
                        QueryTemplateVariables.SCOPE_ASSIGNED_TO_ME.equals(scope)
                                ? ExpenseAgentQueryPort.Scope.ASSIGNED_TO_USER
                                : QueryTemplateVariables.SCOPE_VISIBLE.equals(scope)
                                ? ExpenseAgentQueryPort.Scope.VISIBLE
                                : ExpenseAgentQueryPort.Scope.SUBMITTED_BY_USER,
                        "report",
                        true,
                        variables == null ? null : variables.getStartDate(),
                        variables == null ? null : variables.getEndDate(),
                        status,
                        variables == null ? null : toBigDecimal(variables.getMinAmount()),
                        variables == null ? null : toBigDecimal(variables.getMaxAmount()),
                        variables == null ? null : variables.getNormalizedKeyword(),
                        limit
                );
        ExpenseAgentQueryPort.Aggregate aggregate = expenseQueryPort.aggregateAuthorizedDocuments(
                query, request.getAuthorization());
        long total = aggregate.count();
        List<ExpenseAgentQueryPort.DocumentView> documents = expenseQueryPort.queryAuthorizedDocuments(
                query, request.getAuthorization());

        List<Map<String, Object>> records = new ArrayList<>();
        List<String> evidenceRefs = new ArrayList<>();
        for (ExpenseAgentQueryPort.DocumentView document : documents) {
            Map<String, Object> record = toRecord(document);
            records.add(record);
            evidenceRefs.add(String.valueOf(record.get("evidenceRef")));
        }

        return ControlledToolResult.builder()
                .success(true)
                .summary("符合条件共 " + total + " 条报销单，展示 " + records.size() + " 条")
                .records(records)
                .evidenceRefs(evidenceRefs)
                .totalCount(total)
                .totalAmount(aggregate.totalAmount())
                .queryDescriptor(query)
                .sourceDigest(expenseQueryPort.fingerprintAuthorizedDocuments(query, request.getAuthorization()))
                .build();
    }

    private String resolveScope(String queryTemplateCode, QueryTemplateVariables variables) {
        if (QUERY_TEMPLATE_CODE.equals(queryTemplateCode)) {
            return QueryTemplateVariables.SCOPE_SUBMITTED_BY_ME;
        }
        String scope = variables == null ? null : normalizeText(variables.getScope());
        if (scope == null) {
            return QueryTemplateVariables.SCOPE_VISIBLE;
        }
        if (QueryTemplateVariables.SCOPE_SUBMITTED_BY_ME.equals(scope)
                || QueryTemplateVariables.SCOPE_ASSIGNED_TO_ME.equals(scope)
                || QueryTemplateVariables.SCOPE_VISIBLE.equals(scope)) {
            return scope;
        }
        return null;
    }

    private ControlledToolResult failed(String message) {
        return ControlledToolResult.builder()
                .success(false)
                .errorMessage(message)
                .records(List.of())
                .evidenceRefs(List.of())
                .build();
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private Map<String, Object> toRecord(ExpenseAgentQueryPort.DocumentView document) {
        Map<String, Object> record = new LinkedHashMap<>();
        String evidenceRef = "EXPENSE_DOCUMENT:" + document.id();
        record.put("evidenceRef", evidenceRef);
        record.put("entityRef", evidenceRef);
        record.put("documentId", document.id());
        record.put("documentCode", document.documentCode());
        record.put("title", document.title());
        record.put("reason", document.reason());
        record.put("amount", document.amount());
        record.put("status", document.status());
        record.put("statusLabel", translateStatus(document.status()));
        record.put("templateName", document.templateName());
        record.put("createdAt", document.createdAt() == null ? null : DATE_FORMATTER.format(document.createdAt()));
        return record;
    }

    private String translateStatus(String status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case "DRAFT" -> "草稿";
            case "PENDING_APPROVAL" -> "审批中";
            case "APPROVED" -> "已审批";
            case "REJECTED" -> "已驳回";
            case "PENDING_PAYMENT" -> "待支付";
            case "PAYING" -> "支付中";
            case "PAYMENT_COMPLETED" -> "已支付";
            case "PAYMENT_FINISHED" -> "已完成";
            case "EXCEPTION" -> "流程异常";
            case "PAYMENT_EXCEPTION" -> "支付异常";
            default -> status;
        };
    }
}
