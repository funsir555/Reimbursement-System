package com.finex.auth.service.digitalemployee;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import com.finex.auth.agent.workflow.ControlledQueryCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validated, templated schedule definitions and per-user opt-in. */
@Service
@RequiredArgsConstructor
public class DigitalEmployeePlanService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final DigitalEmployeeService employees;
    private final QueryAuthorizationService authorization;
    private final ControlledMessageAccessService messageAccess;

    public record PlanRequest(Long workflowBindingId, String frequency, Integer runHour,
                              Integer runMinute, Integer weekDay, Integer monthDay,
                              Map<String, Object> parameters, Long versionNo, boolean enabled) { }
    public record SubscriptionRequest(boolean enabled) { }

    public List<Map<String, Object>> plans(Long employeeId) {
        employees.detail(employeeId);
        return jdbc.queryForList("""
                SELECT p.*, (SELECT COUNT(*) FROM ea_digital_employee_subscription s
                WHERE s.plan_id=p.id AND s.enabled=1) subscriber_count
                FROM ea_digital_employee_plan p WHERE p.employee_id=? ORDER BY p.id
                """, employeeId);
    }

    @Transactional
    public Map<String, Object> save(Long employeeId, Long planId, PlanRequest request, Long actorId) {
        Map<String, Object> employee = employees.detail(employeeId);
        validate(request, employeeId);
        String status = request.enabled() ? "ACTIVE" : "DRAFT";
        if (request.enabled() && (employee.get("daily_run_limit") == null || employee.get("concurrency_limit") == null))
            throw new IllegalStateException("启用计划前请配置每日运行上限与并发上限");
        LocalDateTime nextRun = request.enabled() ? nextRun(request, LocalDateTime.now(ZONE)) : null;
        String parameters = writeJson(request.parameters());
        Long id = planId;
        if (id == null) {
            GeneratedKeyHolder keys = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO ea_digital_employee_plan
                        (employee_id,workflow_binding_id,frequency,run_hour,run_minute,week_day,month_day,
                         parameter_json,status,next_run_at)
                        VALUES (?,?,?,?,?,?,?,?,?,?)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, employeeId);
                statement.setLong(2, request.workflowBindingId());
                statement.setString(3, request.frequency());
                statement.setInt(4, request.runHour());
                statement.setInt(5, request.runMinute());
                statement.setObject(6, request.weekDay());
                statement.setObject(7, request.monthDay());
                statement.setString(8, parameters);
                statement.setString(9, status);
                statement.setObject(10, nextRun);
                return statement;
            }, keys);
            id = keys.getKey().longValue();
        } else {
            int updated = jdbc.update("""
                    UPDATE ea_digital_employee_plan SET workflow_binding_id=?,frequency=?,run_hour=?,run_minute=?,
                    week_day=?,month_day=?,parameter_json=?,status=?,next_run_at=?,version_no=version_no+1
                    WHERE id=? AND employee_id=? AND version_no=?
                    """, request.workflowBindingId(), request.frequency(), request.runHour(), request.runMinute(),
                    request.weekDay(), request.monthDay(), parameters, status, nextRun,
                    id, employeeId, request.versionNo());
            if (updated != 1) throw new IllegalStateException("计划已被其他人修改，请刷新后重试");
        }
        employees.audit(employeeId, actorId, "PLAN", "计划：" + id + "，状态：" + status);
        return requirePlan(id);
    }

    public List<Map<String, Object>> subscriptions(Long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.id plan_id,p.employee_id,p.workflow_binding_id,p.frequency,p.run_hour,p.run_minute,p.week_day,p.month_day,
                       p.status,p.next_run_at,e.name employee_name,d.agent_name,
                       COALESCE(s.enabled,0) subscribed
                FROM ea_digital_employee_plan p
                JOIN ea_digital_employee_user u ON u.employee_id=p.employee_id AND u.user_id=?
                JOIN ea_digital_employee e ON e.id=p.employee_id AND e.tenant_id=?
                JOIN ea_digital_employee_service es ON es.employee_id=e.id AND es.lifecycle_status='ACTIVE'
                JOIN ea_digital_employee_workflow b ON b.id=p.workflow_binding_id AND b.employee_id=e.id AND b.status=1
                JOIN ea_workflow_share sh ON sh.id=b.share_id AND sh.status='APPROVED'
                  AND sh.permission_code=b.permission_code
                JOIN ea_agent_definition d ON d.id=sh.agent_id AND d.status='READY'
                JOIN ea_agent_version v ON v.id=sh.agent_version_id AND v.agent_id=sh.agent_id AND v.published=1
                LEFT JOIN ea_digital_employee_subscription s ON s.plan_id=p.id AND s.user_id=?
                WHERE p.status='ACTIVE' ORDER BY p.id
                """, userId, TenantContext.getEffectiveTenantId(), userId);
        Set<Long> availableBindings = employees.availableAuthorizedFor(userId).stream()
                .map(row -> ((Number) row.get("binding_id")).longValue()).collect(java.util.stream.Collectors.toSet());
        return rows.stream().filter(row -> availableBindings.contains(
                ((Number) row.get("workflow_binding_id")).longValue())).toList();
    }

    @Transactional
    public void subscribe(Long planId, Long userId, boolean enabled) {
        boolean available = subscriptions(userId).stream().anyMatch(item -> planId.equals(((Number) item.get("plan_id")).longValue()));
        if (!available) throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        jdbc.update("""
                INSERT INTO ea_digital_employee_subscription (plan_id,user_id,enabled) VALUES (?,?,?)
                ON DUPLICATE KEY UPDATE enabled=VALUES(enabled)
                """, planId, userId, enabled ? 1 : 0);
    }

    public List<Map<String, Object>> inbox(Long userId) {
        List<Map<String, Object>> deliveries = jdbc.queryForList("""
                SELECT d.id,d.plan_id,d.employee_id,d.scheduled_at,d.status,d.permission_code,
                       d.agent_version_id,d.result_text,d.auth_digest,d.origin_required,
                       d.failure_code,d.created_at,
                       e.name employee_name
                FROM ea_digital_employee_delivery d JOIN ea_digital_employee e ON e.id=d.employee_id
                WHERE d.user_id=? AND e.tenant_id=? ORDER BY d.created_at DESC,d.id DESC LIMIT 100
                """, userId, TenantContext.getEffectiveTenantId());
        for (Map<String, Object> delivery : deliveries) {
            if (!canReadDelivery(userId, delivery)) {
                delivery.put("result_text", null);
                delivery.put("status", "NO_PERMISSION");
            }
        }
        return deliveries;
    }

    private boolean canReadDelivery(Long userId, Map<String, Object> delivery) {
        if (delivery.get("result_text") == null) return true;
        List<Map<String, Object>> origins = jdbc.queryForList("""
                SELECT * FROM ea_controlled_result_origin
                WHERE source_type='DELIVERY' AND source_id=? ORDER BY item_no
                """, delivery.get("id"));
        if (!origins.isEmpty()) {
            if (origins.size() != ((Number) origins.get(0).get("origin_count")).intValue()) return false;
            return origins.stream().allMatch(origin -> messageAccess.canReadToolOrigin(userId, origin));
        }
        if (Boolean.TRUE.equals(delivery.get("origin_required"))
                || delivery.get("origin_required") instanceof Number flag && flag.intValue() == 1)
            return false;
        Long employeeId = ((Number) delivery.get("employee_id")).longValue();
        Long versionId = ((Number) delivery.get("agent_version_id")).longValue();
        String permissionCode = (String) delivery.get("permission_code");
        boolean binding = employees.availableFor(userId).stream().anyMatch(candidate ->
                employeeId.equals(((Number) candidate.get("id")).longValue())
                        && versionId.equals(((Number) candidate.get("agent_version_id")).longValue())
                        && permissionCode.equals(candidate.get("permission_code")));
        if (!binding) return false;
        try {
            List<String> limit = employees.companyLimit(employeeId, permissionCode);
            Long responsibleId = ((Number) employees.detail(employeeId).get("responsible_user_id")).longValue();
            return authorization.resolve(userId, responsibleId, permissionCode,
                    limit.isEmpty() ? null : Set.copyOf(limit)).digest().equals(delivery.get("auth_digest"));
        } catch (SecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    public Map<String, Object> requirePlan(Long planId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM ea_digital_employee_plan WHERE id=?", planId);
        if (rows.size() != 1) throw new IllegalArgumentException("定时计划不存在");
        return rows.get(0);
    }

    public LocalDateTime nextRun(PlanRequest plan, LocalDateTime after) {
        LocalDate date = after.toLocalDate();
        LocalTime time = LocalTime.of(plan.runHour(), plan.runMinute());
        for (int offset = 0; offset <= 35; offset++) {
            LocalDate candidate = date.plusDays(offset);
            boolean accepted = switch (plan.frequency()) {
                case "DAILY" -> true;
                case "WEEKLY" -> candidate.getDayOfWeek().getValue() == plan.weekDay();
                case "MONTHLY" -> candidate.getDayOfMonth() == plan.monthDay();
                default -> false;
            };
            LocalDateTime dateTime = candidate.atTime(time);
            if (accepted && dateTime.isAfter(after)) return dateTime;
        }
        throw new IllegalStateException("无法计算下一次运行时间");
    }

    public Map<String, Object> parameters(String raw) {
        try { return json.readValue(raw, new TypeReference<>() { }); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("计划参数不是有效 JSON", exception); }
    }

    private void validate(PlanRequest request, Long employeeId) {
        if (request == null || request.workflowBindingId() == null || request.frequency() == null
                || !Set.of("DAILY", "WEEKLY", "MONTHLY").contains(request.frequency())
                || request.runHour() == null || request.runHour() < 0 || request.runHour() > 23
                || request.runMinute() == null || request.runMinute() < 0 || request.runMinute() > 59)
            throw new IllegalArgumentException("计划频率或执行时间无效");
        if ("WEEKLY".equals(request.frequency()) && (request.weekDay() == null || request.weekDay() < 1 || request.weekDay() > 7))
            throw new IllegalArgumentException("请选择周一至周日");
        if ("MONTHLY".equals(request.frequency()) && (request.monthDay() == null || request.monthDay() < 1 || request.monthDay() > 28))
            throw new IllegalArgumentException("每月执行日仅支持 1 至 28 日");
        List<Map<String, Object>> bindings = jdbc.queryForList("""
                SELECT b.permission_code,b.share_id,v.config_json FROM ea_digital_employee_workflow b
                JOIN ea_workflow_share sh ON sh.id=b.share_id AND sh.status='APPROVED'
                  AND sh.permission_code=b.permission_code
                JOIN ea_agent_definition d ON d.id=sh.agent_id AND d.status='READY'
                JOIN ea_agent_version v ON v.id=sh.agent_version_id AND v.agent_id=sh.agent_id AND v.published=1
                WHERE b.id=? AND b.employee_id=? AND b.status=1
                """, request.workflowBindingId(), employeeId);
        if (bindings.size() != 1) throw new IllegalArgumentException("计划必须绑定已审核的查询工作流");
        String permission = (String) bindings.get(0).get("permission_code");
        Map<String, Object> params = request.parameters() == null ? Map.of() : request.parameters();
        List<String> capabilities = jdbc.queryForList("""
                SELECT permission_code FROM ea_workflow_share_capability WHERE share_id=?
                """, String.class, bindings.get(0).get("share_id"));
        if (capabilities.isEmpty()) throw new IllegalArgumentException("固定版本缺少查询能力声明");
        if (capabilities.size() > 1 || !Set.of(QueryAuthorizationService.EXPENSE_QUERY,
                QueryAuthorizationService.FINANCE_CLOSE_QUERY).contains(permission)) {
            validateControlledPlan(params, String.valueOf(bindings.get(0).get("config_json")));
            return;
        }
        if (QueryAuthorizationService.FINANCE_CLOSE_QUERY.equals(permission)) {
            if (!params.keySet().equals(Set.of("year", "period"))
                    || !(params.get("year") instanceof Number year) || year.intValue() < 2000 || year.intValue() > 2100
                    || !(params.get("period") instanceof Number period) || period.intValue() < 1 || period.intValue() > 12)
                throw new IllegalArgumentException("财务模板需要有效的 year 和 period");
        } else if (QueryAuthorizationService.EXPENSE_QUERY.equals(permission)) {
            if (!params.isEmpty() && !params.keySet().equals(Set.of("scope")))
                throw new IllegalArgumentException("报销模板仅支持 scope 参数");
            if (!params.isEmpty() && !Set.of("MY", "ASSIGNED", "VISIBLE").contains(params.get("scope")))
                throw new IllegalArgumentException("报销查询范围参数无效");
        } else throw new IllegalArgumentException("不支持的定时查询能力");
    }

    private void validateControlledPlan(Map<String, Object> params, String configJson) {
        try {
            var root = json.readTree(configJson);
            var nodes = root.path("workflow").path("nodes");
            if (!nodes.isArray()) throw new IllegalArgumentException("固定版本没有有效流程节点");
            Set<String> found = new java.util.HashSet<>();
            for (var node : nodes) {
                if (!"tool".equalsIgnoreCase(node.path("nodeType").asText())) continue;
                String key = node.path("nodeKey").asText();
                String code = node.path("config").path("toolCode").asText();
                var definition = ControlledQueryCatalog.require(code);
                found.add(key);
                Object nodeValues = params.get(key);
                if (!(nodeValues instanceof Map<?, ?> values))
                    throw new IllegalArgumentException("定时模板缺少工具节点参数：" + key);
                Set<String> allowed = definition.fields().stream()
                        .map(ControlledQueryCatalog.Field::key)
                        .collect(java.util.stream.Collectors.toSet());
                if ("EXPENSE".equals(definition.kind())) allowed = Set.of("scope", "keyword", "status");
                if ("CLOSE".equals(definition.kind())) allowed = Set.of("year", "period");
                if (!allowed.containsAll(values.keySet()))
                    throw new IllegalArgumentException("定时模板含有未知工具参数");
                if ("CLOSE".equals(definition.kind())) {
                    int year = planInteger(values.get("year"), "年度");
                    int period = planInteger(values.get("period"), "期间");
                    if (year < 2000 || year > 2099 || period < 1 || period > 12)
                        throw new IllegalArgumentException("未结账查询期间无效");
                } else if ("EXPENSE".equals(definition.kind())) {
                    if (values.containsKey("scope") && !Set.of("MY", "ASSIGNED", "VISIBLE")
                            .contains(String.valueOf(values.get("scope"))))
                        throw new IllegalArgumentException("报销查询范围参数无效");
                } else for (var field : definition.fields()) {
                    if (field.required() && (values.get(field.key()) == null
                            || String.valueOf(values.get(field.key())).isBlank()))
                        throw new IllegalArgumentException("定时模板缺少必要参数：" + field.label());
                }
                if ("BALANCE".equals(definition.kind()) || "SEQUENCE".equals(definition.kind())) {
                    int fromYear = planInteger(values.get("yearFrom"), "起始年度");
                    int fromPeriod = planInteger(values.get("periodFrom"), "起始月份");
                    int toYear = planInteger(values.get("yearTo"), "结束年度");
                    int toPeriod = planInteger(values.get("periodTo"), "结束月份");
                    if (fromYear < 1990 || toYear > 2090 || fromPeriod < 1 || fromPeriod > 12
                            || toPeriod < 1 || toPeriod > 12
                            || java.time.YearMonth.of(fromYear, fromPeriod)
                                    .isAfter(java.time.YearMonth.of(toYear, toPeriod)))
                        throw new IllegalArgumentException("账务查询会计期间无效");
                    if (!Set.of("POSTED_ONLY", "INCLUDE_SAVED_UNPOSTED")
                            .contains(String.valueOf(values.get("voucherStatus"))))
                        throw new IllegalArgumentException("请选择凭证范围");
                }
                if ("PEOPLE".equals(definition.kind())) {
                    String name = String.valueOf(values.get("name"));
                    if (name.codePointCount(0, name.length()) < 2)
                        throw new IllegalArgumentException("人员姓名至少填写两个字符");
                }
            }
            if (found.isEmpty() || !found.containsAll(params.keySet()))
                throw new IllegalArgumentException("定时模板包含未知工具节点");
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("固定版本流程配置无效", exception);
        }
    }

    private int planInteger(Object value, String label) {
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + "必须是整数");
        }
    }

    private String writeJson(Object value) {
        try { return json.writeValueAsString(value == null ? Map.of() : value); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("计划参数无法保存", exception); }
    }
}
