package com.finex.auth.service.digitalemployee;

import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.agent.context.TraceContext;
import com.finex.auth.agent.workflow.AgentWorkflowRuntimeService;
import com.finex.auth.agent.workflow.WorkflowExecutionResult;
import com.finex.auth.agent.workflow.WorkflowSelectorService;
import com.finex.auth.agent.workflow.ControlledQueryCatalog;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Synchronous per-user runs; unique delivery key and conditional due update prevent repeats. */
@Service
@RequiredArgsConstructor
@Slf4j
public class DigitalEmployeeScheduleRunner {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private final JdbcTemplate jdbc;
    private final DigitalEmployeePlanService plans;
    private final DigitalEmployeeService employees;
    private final QueryAuthorizationService authorization;
    private final AgentWorkflowRuntimeService runtime;
    private final WorkflowSelectorService selector;

    @Scheduled(fixedDelay = 60000L)
    public void runDue() {
        List<Map<String, Object>> due = jdbc.queryForList("""
                SELECT p.*,e.tenant_id,e.responsible_user_id,s.daily_run_limit,s.concurrency_limit,
                       b.permission_code,sh.agent_version_id,
                       CASE WHEN b.status=1 AND sh.status='APPROVED'
                             AND sh.permission_code=b.permission_code
                             AND d.status='READY' AND v.published=1 THEN 1 ELSE 0 END binding_valid
                FROM ea_digital_employee_plan p
                JOIN ea_digital_employee e ON e.id=p.employee_id
                JOIN ea_digital_employee_service s ON s.employee_id=e.id AND s.lifecycle_status='ACTIVE'
                LEFT JOIN ea_digital_employee_workflow b ON b.id=p.workflow_binding_id AND b.employee_id=e.id
                LEFT JOIN ea_workflow_share sh ON sh.id=b.share_id
                LEFT JOIN ea_agent_definition d ON d.id=sh.agent_id
                LEFT JOIN ea_agent_version v ON v.id=sh.agent_version_id AND v.agent_id=sh.agent_id
                WHERE p.status='ACTIVE' AND p.next_run_at<=? ORDER BY p.next_run_at,p.id LIMIT 20
                """, LocalDateTime.now(ZONE));
        for (Map<String, Object> plan : due) {
            try { runPlan(plan); }
            catch (Exception exception) {
                log.error("数字员工计划调度失败: planId={}", plan.get("id"), exception);
            } finally { TenantContext.clear(); TraceContext.clear(); }
        }
    }

    private void runPlan(Map<String, Object> plan) {
        Long planId = ((Number) plan.get("id")).longValue();
        Long employeeId = ((Number) plan.get("employee_id")).longValue();
        TenantContext.setTenantId((String) plan.get("tenant_id"));
        if (!(plan.get("binding_valid") instanceof Number valid) || valid.intValue() != 1) {
            jdbc.update("UPDATE ea_digital_employee_plan SET status='PAUSED_AUTHORIZATION' WHERE id=? AND status='ACTIVE'",
                    planId);
            employees.audit(employeeId, ((Number) plan.get("responsible_user_id")).longValue(),
                    "AUTH_PAUSE", "固定版本已停用或共享审核失效：计划 " + planId);
            return;
        }
        LocalDateTime scheduledAt = ((java.sql.Timestamp) plan.get("next_run_at")).toLocalDateTime();
        LocalDateTime next = plans.nextRun(new DigitalEmployeePlanService.PlanRequest(
                ((Number) plan.get("workflow_binding_id")).longValue(), (String) plan.get("frequency"),
                ((Number) plan.get("run_hour")).intValue(), ((Number) plan.get("run_minute")).intValue(),
                plan.get("week_day") == null ? null : ((Number) plan.get("week_day")).intValue(),
                plan.get("month_day") == null ? null : ((Number) plan.get("month_day")).intValue(),
                plans.parameters((String) plan.get("parameter_json")),
                ((Number) plan.get("version_no")).longValue(), true), scheduledAt);
        int claimed = jdbc.update("""
                UPDATE ea_digital_employee_plan SET next_run_at=? WHERE id=? AND status='ACTIVE' AND next_run_at=?
                """, next, planId, scheduledAt);
        if (claimed != 1) return;
        List<Long> subscribers = jdbc.queryForList("""
                SELECT s.user_id FROM ea_digital_employee_subscription s
                JOIN ea_digital_employee_user u ON u.employee_id=? AND u.user_id=s.user_id
                JOIN sys_user person ON person.id=s.user_id AND person.status=1
                WHERE s.plan_id=? AND s.enabled=1 ORDER BY s.user_id
                """, Long.class, employeeId, planId);
        int cap = ((Number) plan.get("daily_run_limit")).intValue();
        int concurrency = ((Number) plan.get("concurrency_limit")).intValue();
        for (Long userId : subscribers) {
            Integer today = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ea_digital_employee_delivery
                    WHERE employee_id=? AND created_at>=CURRENT_DATE()
                    """, Integer.class, employeeId);
            Integer running = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ea_digital_employee_delivery
                    WHERE employee_id=? AND status='RUNNING'
                    """, Integer.class, employeeId);
            if (today == null || today >= cap || running == null || running >= concurrency) {
                jdbc.update("UPDATE ea_digital_employee_plan SET status='PAUSED_BUDGET' WHERE id=?", planId);
                employees.audit(employeeId, ((Number) plan.get("responsible_user_id")).longValue(),
                        "BUDGET_PAUSE", "计划：" + planId);
                return;
            }
            runForUser(plan, userId, scheduledAt);
        }
    }

    private void runForUser(Map<String, Object> plan, Long userId, LocalDateTime scheduledAt) {
        Long planId = ((Number) plan.get("id")).longValue();
        Long employeeId = ((Number) plan.get("employee_id")).longValue();
        Long bindingId = ((Number) plan.get("workflow_binding_id")).longValue();
        Long responsible = ((Number) plan.get("responsible_user_id")).longValue();
        Long versionId = ((Number) plan.get("agent_version_id")).longValue();
        String permission = (String) plan.get("permission_code");
        String traceId = "schedule-" + UUID.randomUUID();
        int inserted = jdbc.update("""
                INSERT IGNORE INTO ea_digital_employee_delivery
                (plan_id,user_id,scheduled_at,status,permission_code,employee_id,agent_version_id,trace_id)
                VALUES (?,?,?,'RUNNING',?,?,?,?)
                """, planId, userId, scheduledAt, permission, employeeId, versionId, traceId);
        if (inserted != 1) return;
        TenantContext.setUserId(userId);
        List<String> homeCompanies = jdbc.queryForList("SELECT company_id FROM sys_user WHERE id=? AND status=1",
                String.class, userId);
        TenantContext.setCompanyId(homeCompanies.isEmpty() ? null : homeCompanies.get(0));
        TraceContext.setTraceId(traceId);
        try {
            var workflow = selector.authorizedCandidates(userId, employeeId, bindingId).stream()
                    .filter(candidate -> bindingId.equals(candidate.getWorkflowBindingId()))
                    .findFirst().orElseThrow(() -> new SecurityException(QueryAuthorizationService.NO_PERMISSION));
            Map<String, QueryAuthorization> authorized = new java.util.LinkedHashMap<>();
            for (String code : workflow.getQueryPermissionCodes()) {
                List<String> limit = employees.companyLimit(employeeId, code);
                authorized.put(code, authorization.resolve(userId, responsible, code,
                        limit.isEmpty() ? null : Set.copyOf(limit)));
            }
            Map<String, Object> parameters = plans.parameters((String) plan.get("parameter_json"));
            boolean legacySingle = workflow.getToolCodes().size() == 1 && Set.of(
                    "expense.query_my_expenses", "finance.query_unclosed_companies")
                    .contains(workflow.getToolCode());
            WorkflowExecutionResult result;
            if (legacySingle) {
                String message = templateMessage(permission, parameters);
                String intent = QueryAuthorizationService.FINANCE_CLOSE_QUERY.equals(permission)
                        ? "query_finance_close" : "query_expense";
                result = runtime.executeQueryWorkflow("schedule-" + planId + "-" + userId,
                        userId, message, intent, null, null, employeeId, bindingId);
            } else {
                result = runtime.executePlannedWorkflow("schedule-" + planId + "-" + userId,
                        userId, workflow, "执行已配置的定时受控查询", parameters);
            }
            if (!result.isSuccess() || !authorized.get(permission).digest().equals(result.getAuthorizationDigest())) {
                throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
            }
            for (Map.Entry<String, QueryAuthorization> item : authorized.entrySet()) {
                List<String> limit = employees.companyLimit(employeeId, item.getKey());
                QueryAuthorization current = authorization.resolve(userId, responsible, item.getKey(),
                        limit.isEmpty() ? null : Set.copyOf(limit));
                if (!current.digest().equals(item.getValue().digest()))
                    throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
            }
            if (!legacySingle) {
                if (result.getToolOrigins() == null || result.getToolOrigins().size() != workflow.getToolCodes().size())
                    throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
                Long deliveryId = jdbc.queryForObject("""
                        SELECT id FROM ea_digital_employee_delivery
                        WHERE plan_id=? AND user_id=? AND scheduled_at=?
                        """, Long.class, planId, userId, scheduledAt);
                int index = 1;
                for (WorkflowExecutionResult.ToolOrigin origin : result.getToolOrigins()) jdbc.update("""
                        INSERT INTO ea_controlled_result_origin
                        (source_type,source_id,item_no,origin_count,employee_id,binding_id,responsible_user_id,
                         agent_version_id,tool_code,permission_code,auth_digest,query_json,source_digest)
                        VALUES ('DELIVERY',?,?,?,?,?,?,?,?,?,?,?,?)
                        """, deliveryId, index++, result.getToolOrigins().size(), employeeId, bindingId,
                        responsible, versionId, origin.toolCode(), origin.permissionCode(),
                        origin.authorizationDigest(), origin.queryDescriptorJson(), origin.sourceDigest());
            }
            jdbc.update("""
                    UPDATE ea_digital_employee_delivery
                    SET status=?,result_text=?,auth_digest=?,origin_required=?,finished_at=NOW()
                    WHERE plan_id=? AND user_id=? AND scheduled_at=? AND status='RUNNING'
                    """, result.getResultCount() != null && result.getResultCount() == 0 ? "ZERO" : "SUCCESS",
                    result.getContent(), authorized.get(permission).digest(), legacySingle ? 0 : 1,
                    planId, userId, scheduledAt);
        } catch (Exception exception) {
            String failure = exception instanceof SecurityException ? "NO_PERMISSION" : "EXECUTION_FAILED";
            jdbc.update("""
                    UPDATE ea_digital_employee_delivery SET status=?,failure_code=?,result_text=NULL,finished_at=NOW()
                    WHERE plan_id=? AND user_id=? AND scheduled_at=? AND status='RUNNING'
                    """, failure, failure, planId, userId, scheduledAt);
            log.warn("数字员工定时查询未送达: planId={}, userId={}, failure={}", planId, userId, failure);
        } finally {
            TenantContext.setUserId(null);
            TenantContext.setCompanyId(null);
            TraceContext.clear();
        }
    }

    private String templateMessage(String permission, Map<String, Object> parameters) {
        if (QueryAuthorizationService.FINANCE_CLOSE_QUERY.equals(permission))
            return "请统计" + parameters.get("year") + "年" + parameters.get("period") + "月未结账公司数";
        return switch (String.valueOf(parameters.getOrDefault("scope", "MY"))) {
            case "ASSIGNED" -> "查询待我处理的报销单";
            case "VISIBLE" -> "查询全部公司报销单";
            default -> "查询我的报销单";
        };
    }
}
