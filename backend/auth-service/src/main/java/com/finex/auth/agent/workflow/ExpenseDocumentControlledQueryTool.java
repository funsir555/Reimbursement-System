package com.finex.auth.agent.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
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

    private final ProcessDocumentInstanceMapper documentMapper;
    private final ProcessDocumentTaskMapper taskMapper;

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
        if (request.getUserId() == null) {
            return ControlledToolResult.builder()
                    .success(false)
                    .errorMessage("无法获取当前用户")
                    .records(List.of())
                    .evidenceRefs(List.of())
                    .build();
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

        LambdaQueryWrapper<ProcessDocumentInstance> wrapper = new LambdaQueryWrapper<>();
        if (QueryTemplateVariables.SCOPE_ASSIGNED_TO_ME.equals(scope)) {
            List<String> assignedDocumentCodes = taskMapper.selectList(
                    new LambdaQueryWrapper<ProcessDocumentTask>()
                            .eq(ProcessDocumentTask::getAssigneeUserId, request.getUserId())
                            .eq(ProcessDocumentTask::getStatus, "PENDING")
                            .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
            ).stream()
                    .map(ProcessDocumentTask::getDocumentCode)
                    .filter(code -> code != null && !code.isBlank())
                    .distinct()
                    .toList();
            if (assignedDocumentCodes.isEmpty()) {
                return emptyResult();
            }
            wrapper.in(ProcessDocumentInstance::getDocumentCode, assignedDocumentCodes);
        } else {
            wrapper.eq(ProcessDocumentInstance::getSubmitterUserId, request.getUserId());
        }
        wrapper.eq(ProcessDocumentInstance::getTemplateType, "report");
        wrapper.and(inner -> inner.isNull(ProcessDocumentInstance::getDeleted)
                .or()
                .eq(ProcessDocumentInstance::getDeleted, Boolean.FALSE));

        if (status != null) {
            wrapper.eq(ProcessDocumentInstance::getStatus, status);
        }
        if (variables != null && variables.getStartDate() != null) {
            LocalDateTime start = LocalDateTime.ofInstant(variables.getStartDate(), ZoneId.systemDefault());
            wrapper.ge(ProcessDocumentInstance::getCreatedAt, start);
        }
        if (variables != null && variables.getEndDate() != null) {
            LocalDateTime end = LocalDateTime.ofInstant(variables.getEndDate(), ZoneId.systemDefault());
            wrapper.le(ProcessDocumentInstance::getCreatedAt, end);
        }
        if (variables != null && variables.getMinAmount() != null) {
            wrapper.ge(ProcessDocumentInstance::getTotalAmount, variables.getMinAmount());
        }
        if (variables != null && variables.getMaxAmount() != null) {
            wrapper.le(ProcessDocumentInstance::getTotalAmount, variables.getMaxAmount());
        }
        if (variables != null && variables.getNormalizedKeyword() != null) {
            String keyword = variables.getNormalizedKeyword();
            wrapper.and(inner -> inner
                    .like(ProcessDocumentInstance::getDocumentCode, keyword)
                    .or()
                    .like(ProcessDocumentInstance::getDocumentTitle, keyword)
                    .or()
                    .like(ProcessDocumentInstance::getDocumentReason, keyword)
                    .or()
                    .like(ProcessDocumentInstance::getTemplateName, keyword));
        }

        int limit = variables == null || variables.getLimit() == null ? 10 : Math.min(Math.max(variables.getLimit(), 1), 50);
        wrapper.orderByDesc(ProcessDocumentInstance::getCreatedAt);
        wrapper.last("LIMIT " + limit);

        List<ProcessDocumentInstance> documents = documentMapper.selectList(wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        List<String> evidenceRefs = new ArrayList<>();
        for (ProcessDocumentInstance document : documents) {
            Map<String, Object> record = toRecord(document);
            records.add(record);
            evidenceRefs.add(String.valueOf(record.get("evidenceRef")));
        }

        return ControlledToolResult.builder()
                .success(true)
                .summary("查询到 " + records.size() + " 条报销单")
                .records(records)
                .evidenceRefs(evidenceRefs)
                .build();
    }

    private String resolveScope(String queryTemplateCode, QueryTemplateVariables variables) {
        if (QUERY_TEMPLATE_CODE.equals(queryTemplateCode)) {
            return QueryTemplateVariables.SCOPE_SUBMITTED_BY_ME;
        }
        String scope = variables == null ? null : normalizeText(variables.getScope());
        if (scope == null) {
            return QueryTemplateVariables.SCOPE_SUBMITTED_BY_ME;
        }
        if (QueryTemplateVariables.SCOPE_SUBMITTED_BY_ME.equals(scope)
                || QueryTemplateVariables.SCOPE_ASSIGNED_TO_ME.equals(scope)) {
            return scope;
        }
        return null;
    }

    private ControlledToolResult emptyResult() {
        return ControlledToolResult.builder()
                .success(true)
                .summary("查询到 0 条报销单")
                .records(List.of())
                .evidenceRefs(List.of())
                .build();
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

    private Map<String, Object> toRecord(ProcessDocumentInstance document) {
        Map<String, Object> record = new LinkedHashMap<>();
        String evidenceRef = "EXPENSE_DOCUMENT:" + document.getId();
        record.put("evidenceRef", evidenceRef);
        record.put("entityRef", evidenceRef);
        record.put("documentId", document.getId());
        record.put("documentCode", document.getDocumentCode());
        record.put("title", document.getDocumentTitle());
        record.put("reason", document.getDocumentReason());
        record.put("amount", document.getTotalAmount());
        record.put("status", document.getStatus());
        record.put("statusLabel", translateStatus(document.getStatus()));
        record.put("templateName", document.getTemplateName());
        record.put("createdAt", document.getCreatedAt() == null ? null : DATE_FORMATTER.format(document.getCreatedAt()));
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
