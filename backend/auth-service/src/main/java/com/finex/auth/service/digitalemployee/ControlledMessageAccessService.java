package com.finex.auth.service.digitalemployee;

import com.finex.auth.agent.entity.AgentMessage;
import com.finex.auth.agent.workflow.WorkflowExecutionResult;
import com.finex.auth.agent.workflow.FinWorkOrchestrationService;
import com.finex.auth.agent.workflow.ControlledQueryCatalog;
import com.finex.auth.agent.workflow.ToolExecutionRequest;
import com.finex.auth.agent.workflow.ToolRuntimeService;
import com.finex.auth.agent.workflow.WorkflowExecutionResult.ToolOrigin;
import com.finex.auth.service.port.ExpenseAgentQueryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Keeps only provenance for a controlled reply and rechecks it on every read. */
@Service
@RequiredArgsConstructor
public class ControlledMessageAccessService {
    private final JdbcTemplate jdbc;
    private final DigitalEmployeeService employees;
    private final QueryAuthorizationService authorization;
    private final ExpenseAgentQueryPort expenseQueries;
    private final ObjectMapper json;
    private final ToolRuntimeService tools;

    @Transactional
    public void recordComposite(AgentMessage message,
                                List<FinWorkOrchestrationService.Execution> executions) {
        if (message == null || message.getId() == null || executions == null
                || executions.isEmpty() || executions.size() > 4) {
            throw new IllegalArgumentException("受控回答缺少来源");
        }
        int itemNo = 1;
        int originCount = executions.stream().mapToInt(execution -> {
            List<ToolOrigin> origins = execution.result().getToolOrigins();
            return origins == null || origins.isEmpty() ? 1 : origins.size();
        }).sum();
        for (FinWorkOrchestrationService.Execution execution : executions) {
            WorkflowExecutionResult result = execution.result();
            if (result == null || !result.isSuccess() || result.getDigitalEmployeeId() == null
                    || result.getAgentVersionId() == null || result.getResponsibleUserId() == null)
                throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
            if (result.getToolOrigins() != null && !result.getToolOrigins().isEmpty()) {
                for (ToolOrigin origin : result.getToolOrigins()) {
                    if (origin == null || origin.toolCode() == null || origin.permissionCode() == null
                            || origin.authorizationDigest() == null || origin.queryDescriptorJson() == null
                            || origin.sourceDigest() == null)
                        throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
                    jdbc.update("""
                            INSERT INTO ea_controlled_result_origin
                            (source_type,source_id,item_no,origin_count,employee_id,binding_id,responsible_user_id,
                             agent_version_id,tool_code,permission_code,auth_digest,query_json,source_digest)
                            VALUES ('CHAT',?,?,?,?,?,?,?,?,?,?,?,?)
                            """, message.getId(), itemNo++, originCount, result.getDigitalEmployeeId(),
                            execution.bindingId(), result.getResponsibleUserId(), result.getAgentVersionId(),
                            origin.toolCode(), origin.permissionCode(), origin.authorizationDigest(),
                            origin.queryDescriptorJson(), origin.sourceDigest());
                }
                continue;
            }
            if (!result.isSuccess() || result.getAuthorizationDigest() == null
                    || result.getSourceDigest() == null || result.getQueryDescriptorJson() == null
                    || result.getDigitalEmployeeId() == null || result.getAgentVersionId() == null) {
                throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
            }
            jdbc.update("""
                    INSERT INTO ea_agent_message_query_origin_item
                    (message_id,item_no,origin_count,employee_id,binding_id,responsible_user_id,
                     agent_version_id,permission_code,auth_digest,query_json,source_digest)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?)
                    """, message.getId(), itemNo++, executions.size(), result.getDigitalEmployeeId(),
                    execution.bindingId(), result.getResponsibleUserId(), result.getAgentVersionId(),
                    result.getQueryPermissionCode(), result.getAuthorizationDigest(),
                    result.getQueryDescriptorJson(), result.getSourceDigest());
            jdbc.update("""
                    INSERT INTO ea_controlled_result_origin
                    (source_type,source_id,item_no,origin_count,employee_id,binding_id,responsible_user_id,
                     agent_version_id,tool_code,permission_code,auth_digest,query_json,source_digest)
                    VALUES ('CHAT',?,?,?,?,?,?,?,?,?,?,?,?)
                    """, message.getId(), itemNo - 1, originCount, result.getDigitalEmployeeId(),
                    execution.bindingId(), result.getResponsibleUserId(), result.getAgentVersionId(),
                    result.getQueryPermissionCode().equals(QueryAuthorizationService.EXPENSE_QUERY)
                            ? "expense.query_my_expenses" : "finance.query_unclosed_companies",
                    result.getQueryPermissionCode(), result.getAuthorizationDigest(),
                    result.getQueryDescriptorJson(), result.getSourceDigest());
        }
    }

    public void record(AgentMessage message, WorkflowExecutionResult result) {
        if (message == null || message.getId() == null || result == null || !result.isSuccess()
                || result.getDigitalEmployeeId() == null || result.getAuthorizationDigest() == null) return;
        jdbc.update("""
                INSERT INTO ea_agent_message_query_origin
                (message_id,employee_id,responsible_user_id,agent_version_id,permission_code,auth_digest)
                VALUES (?,?,?,?,?,?)
                """, message.getId(), result.getDigitalEmployeeId(), result.getResponsibleUserId(),
                result.getAgentVersionId(), result.getQueryPermissionCode(), result.getAuthorizationDigest());
    }

    public boolean canRead(Long userId, AgentMessage message) {
        if (message == null || message.getId() == null) return false;
        List<Map<String, Object>> toolOrigins = jdbc.queryForList("""
                SELECT * FROM ea_controlled_result_origin WHERE source_type='CHAT' AND source_id=?
                ORDER BY item_no
                """, message.getId());
        if (!toolOrigins.isEmpty()) {
            if (toolOrigins.size() != ((Number) toolOrigins.get(0).get("origin_count")).intValue()) return false;
            return toolOrigins.stream().allMatch(item -> canReadToolOrigin(userId, item));
        }
        List<Map<String, Object>> composite = jdbc.queryForList(
                "SELECT * FROM ea_agent_message_query_origin_item WHERE message_id=? ORDER BY item_no",
                message.getId());
        if (!composite.isEmpty()) {
            if (composite.size() != ((Number) composite.get(0).get("origin_count")).intValue()) return false;
            return composite.stream().allMatch(item -> canReadOrigin(userId, item));
        }
        List<Map<String, Object>> origins = jdbc.queryForList(
                "SELECT * FROM ea_agent_message_query_origin WHERE message_id=?", message.getId());
        if (origins.size() != 1) return false;
        Map<String, Object> origin = origins.get(0);
        Long employeeId = ((Number) origin.get("employee_id")).longValue();
        Long versionId = ((Number) origin.get("agent_version_id")).longValue();
        String permissionCode = (String) origin.get("permission_code");
        boolean validBinding = employees.availableFor(userId).stream().anyMatch(candidate ->
                employeeId.equals(((Number) candidate.get("id")).longValue())
                        && versionId.equals(((Number) candidate.get("agent_version_id")).longValue())
                        && permissionCode.equals(candidate.get("permission_code")));
        if (!validBinding) return false;
        try {
            List<String> limit = employees.companyLimit(employeeId, permissionCode);
            Long currentResponsible = ((Number) employees.detail(employeeId)
                    .get("responsible_user_id")).longValue();
            if (!currentResponsible.equals(((Number) origin.get("responsible_user_id")).longValue())) return false;
            String currentDigest = authorization.resolve(userId,
                    currentResponsible, permissionCode,
                    limit.isEmpty() ? null : Set.copyOf(limit)).digest();
            return currentDigest.equals(origin.get("auth_digest"));
        } catch (SecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean canReadToolOrigin(Long userId, Map<String, Object> origin) {
        String toolCode = String.valueOf(origin.get("tool_code"));
        if ("expense.query_my_expenses".equals(toolCode)
                || "finance.query_unclosed_companies".equals(toolCode))
            return canReadOrigin(userId, origin);
        try {
            var definition = ControlledQueryCatalog.require(toolCode);
            String permission = String.valueOf(origin.get("permission_code"));
            if (!definition.permission().equals(permission)) return false;
            Long employeeId = number(origin.get("employee_id"));
            Long bindingId = number(origin.get("binding_id"));
            Long versionId = number(origin.get("agent_version_id"));
            Long responsible = number(origin.get("responsible_user_id"));
            boolean binding = employees.availableFor(userId).stream().anyMatch(item ->
                    employeeId.equals(number(item.get("id")))
                    && bindingId.equals(number(item.get("binding_id")))
                    && versionId.equals(number(item.get("agent_version_id")))
                    && jdbc.queryForObject("""
                            SELECT COUNT(*) FROM ea_workflow_share_capability
                            WHERE share_id=? AND permission_code=?
                            """, Integer.class, item.get("share_id"), permission) == 1);
            if (!binding || !responsible.equals(number(employees.detail(employeeId)
                    .get("responsible_user_id")))) return false;
            List<String> limit = employees.companyLimit(employeeId, permission);
            var current = authorization.resolve(userId, responsible, permission,
                    limit.isEmpty() ? null : Set.copyOf(limit));
            if (!current.digest().equals(origin.get("auth_digest"))) return false;
            Map<String, Object> parameters = json.readValue(String.valueOf(origin.get("query_json")),
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.LinkedHashMap<String, Object>>() { });
            parameters.remove("toolCode");
            var result = tools.execute(toolCode, ToolExecutionRequest.builder().userId(userId)
                    .authorization(current).queryTemplateCode(definition.template())
                    .parameters(parameters).build());
            return result.isSuccess() && result.getSourceDigest().equals(origin.get("source_digest"));
        } catch (Exception exception) { return false; }
    }

    private boolean canReadOrigin(Long userId, Map<String, Object> origin) {
        try {
            Long employeeId = number(origin.get("employee_id"));
            Long bindingId = number(origin.get("binding_id"));
            Long versionId = number(origin.get("agent_version_id"));
            Long responsibleId = number(origin.get("responsible_user_id"));
            String permissionCode = String.valueOf(origin.get("permission_code"));
            boolean stillBound = employees.availableFor(userId).stream().anyMatch(item ->
                    employeeId.equals(number(item.get("id")))
                            && bindingId.equals(number(item.get("binding_id")))
                            && versionId.equals(number(item.get("agent_version_id")))
                            && jdbc.queryForObject("""
                                    SELECT COUNT(*) FROM ea_workflow_share_capability
                                    WHERE share_id=? AND permission_code=?
                                    """, Integer.class, item.get("share_id"), permissionCode) == 1);
            if (!stillBound) return false;
            if (!responsibleId.equals(number(employees.detail(employeeId).get("responsible_user_id")))) {
                return false;
            }
            List<String> limit = employees.companyLimit(employeeId, permissionCode);
            var current = authorization.resolve(userId, responsibleId, permissionCode,
                    limit.isEmpty() ? null : Set.copyOf(limit));
            if (!current.digest().equals(origin.get("auth_digest"))) return false;
            if (QueryAuthorizationService.EXPENSE_QUERY.equals(permissionCode)) {
                ExpenseAgentQueryPort.Query query = json.readValue(
                        String.valueOf(origin.get("query_json")), ExpenseAgentQueryPort.Query.class);
                return expenseQueries.fingerprintAuthorizedDocuments(query, current)
                        .equals(origin.get("source_digest"));
            }
            return current.digest().equals(origin.get("source_digest"));
        } catch (Exception exception) {
            return false;
        }
    }

    private Long number(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
