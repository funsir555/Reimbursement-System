package com.finex.auth.service.digitalemployee;

import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Enterprise service configuration. No setting here creates business data permission. */
@Service
@RequiredArgsConstructor
public class DigitalEmployeeService {
    private final JdbcTemplate jdbc;
    private final QueryAuthorizationService authorizationService;

    public record SaveRequest(String name, String description, String avatarCode,
                              Long responsibleUserId, Long versionNo) { }
    public record BindingRequest(Long shareId, String permissionCode, List<String> companyLimit,
                                 Long versionNo, Map<String, List<String>> companyLimits) {
        public BindingRequest(Long shareId, String permissionCode, List<String> companyLimit, Long versionNo) {
            this(shareId, permissionCode, companyLimit, versionNo, null);
        }
    }
    public record AssignmentRequest(List<Long> userIds, Long versionNo) { }
    public record StatusRequest(String status, Long versionNo) { }
    public record BudgetRequest(Integer dailyRunLimit, Integer concurrencyLimit, Long versionNo) { }

    public List<Map<String, Object>> list() {
        return jdbc.queryForList("""
                SELECT e.id,e.name,e.company_id,e.responsible_user_id,s.description,s.avatar_code,
                       s.lifecycle_status,s.daily_run_limit,s.concurrency_limit,s.version_no,
                       (SELECT COUNT(*) FROM ea_digital_employee_user u WHERE u.employee_id=e.id) assigned_count,
                       (SELECT COUNT(*) FROM ea_digital_employee_workflow w WHERE w.employee_id=e.id AND w.status=1) workflow_count,
                       (SELECT COUNT(DISTINCT sub.user_id) FROM ea_digital_employee_subscription sub
                        JOIN ea_digital_employee_plan plan ON plan.id=sub.plan_id
                        WHERE plan.employee_id=e.id AND sub.enabled=1) subscriber_count,
                       (SELECT COUNT(*) FROM ea_digital_employee_delivery delivery
                        WHERE delivery.employee_id=e.id AND delivery.status IN ('NO_PERMISSION','EXECUTION_FAILED')
                          AND delivery.created_at>=DATE_SUB(NOW(), INTERVAL 7 DAY)) exception_count
                FROM ea_digital_employee e JOIN ea_digital_employee_service s ON s.employee_id=e.id
                WHERE e.tenant_id=? ORDER BY e.updated_at DESC,e.id DESC
                """, tenant());
    }

    public Map<String, Object> directory() {
        return Map.of(
                "users", jdbc.queryForList("""
                        SELECT u.id,u.name,u.username,u.company_id FROM sys_user u
                        JOIN sys_company c ON c.company_id=u.company_id AND c.status=1
                        WHERE u.status=1 ORDER BY u.name,u.id
                        """),
                "companies", jdbc.queryForList("""
                        SELECT company_id,company_name FROM sys_company WHERE status=1 ORDER BY company_name
                        """));
    }

    public Map<String, Object> detail(Long employeeId) {
        Map<String, Object> employee = requireEmployee(employeeId);
        employee.put("bindings", jdbc.queryForList("""
                SELECT b.id,b.share_id,b.permission_code,b.status,sh.agent_id,sh.agent_version_id,
                       d.agent_name,d.status agent_status,sh.status share_status,v.published
                FROM ea_digital_employee_workflow b JOIN ea_workflow_share sh ON sh.id=b.share_id
                JOIN ea_agent_definition d ON d.id=sh.agent_id
                JOIN ea_agent_version v ON v.id=sh.agent_version_id AND v.agent_id=sh.agent_id
                WHERE b.employee_id=? ORDER BY b.id
                """, employeeId));
        employee.put("userIds", jdbc.queryForList(
                "SELECT user_id FROM ea_digital_employee_user WHERE employee_id=? ORDER BY user_id",
                Long.class, employeeId));
        employee.put("companyLimits", jdbc.queryForList(
                "SELECT permission_code,company_id FROM ea_digital_employee_scope_company WHERE employee_id=?",
                employeeId));
        return employee;
    }

    @Transactional
    public Map<String, Object> create(SaveRequest request, Long actorId) {
        validateSave(request);
        String companyId = userCompany(actorId);
        requireActiveUser(request.responsibleUserId());
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO ea_digital_employee
                    (tenant_id,company_id,name,type,responsible_user_id,authority_level,status,created_by)
                    VALUES (?,?,?,'咨询型',?,0,'suspended',?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, tenant());
            statement.setString(2, companyId);
            statement.setString(3, request.name().trim());
            statement.setLong(4, request.responsibleUserId());
            statement.setLong(5, actorId);
            return statement;
        }, keys);
        Long id = keys.getKey().longValue();
        jdbc.update("""
                INSERT INTO ea_digital_employee_service (employee_id,description,avatar_code,lifecycle_status)
                VALUES (?,?,?,'DRAFT')
                """, id, request.description(), request.avatarCode());
        audit(id, actorId, "CREATE", "创建草稿");
        return detail(id);
    }

    @Transactional
    public Map<String, Object> update(Long id, SaveRequest request, Long actorId) {
        validateSave(request);
        requireEmployee(id);
        requireActiveUser(request.responsibleUserId());
        int updated = jdbc.update("""
                UPDATE ea_digital_employee_service SET description=?,avatar_code=?,version_no=version_no+1
                WHERE employee_id=? AND version_no=? AND lifecycle_status<>'ARCHIVED'
                """, request.description(), request.avatarCode(), id, request.versionNo());
        if (updated != 1) throw new IllegalStateException("配置已被其他人修改，请刷新后重试");
        jdbc.update("UPDATE ea_digital_employee SET name=?,responsible_user_id=? WHERE id=? AND tenant_id=?",
                request.name().trim(), request.responsibleUserId(), id, tenant());
        audit(id, actorId, "UPDATE", "更新基本信息");
        return detail(id);
    }

    @Transactional
    public Map<String, Object> changeStatus(Long id, StatusRequest request, Long actorId) {
        Map<String, Object> employee = requireEmployee(id);
        String target = request == null ? null : request.status();
        if (target == null || !Set.of("ACTIVE", "SUSPENDED", "ARCHIVED").contains(target)) {
            throw new IllegalArgumentException("不支持的数字员工状态");
        }
        if ("ARCHIVED".equals(employee.get("lifecycle_status"))) throw new IllegalStateException("已归档数字员工不可恢复");
        if ("ACTIVE".equals(target)) {
            requireActiveUser(((Number) employee.get("responsible_user_id")).longValue());
            Integer bindings = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ea_digital_employee_workflow b
                    JOIN ea_workflow_share sh ON sh.id=b.share_id AND sh.status='APPROVED'
                      AND sh.permission_code=b.permission_code
                    JOIN ea_agent_definition d ON d.id=sh.agent_id AND d.status='READY'
                    JOIN ea_agent_version v ON v.id=sh.agent_version_id AND v.agent_id=sh.agent_id AND v.published=1
                    WHERE b.employee_id=? AND b.status=1
                    """, Integer.class, id);
            Integer assignees = jdbc.queryForObject("SELECT COUNT(*) FROM ea_digital_employee_user WHERE employee_id=?",
                    Integer.class, id);
            if (bindings == null || bindings == 0 || assignees == null || assignees == 0)
                throw new IllegalStateException("启用前需绑定已审核工作流并分配使用者");
            Integer schedules = jdbc.queryForObject("SELECT COUNT(*) FROM ea_digital_employee_plan WHERE employee_id=? AND status='ACTIVE'",
                    Integer.class, id);
            if (schedules != null && schedules > 0 &&
                    (employee.get("daily_run_limit") == null || employee.get("concurrency_limit") == null))
                throw new IllegalStateException("启用定时计划前必须设置运行上限和并发上限");
        }
        int updated = jdbc.update("""
                UPDATE ea_digital_employee_service SET lifecycle_status=?,version_no=version_no+1
                WHERE employee_id=? AND version_no=?
                """, target, id, request.versionNo());
        if (updated != 1) throw new IllegalStateException("配置已被其他人修改，请刷新后重试");
        jdbc.update("UPDATE ea_digital_employee SET status=? WHERE id=? AND tenant_id=?",
                "ACTIVE".equals(target) ? "active" : "suspended", id, tenant());
        audit(id, actorId, "STATUS", target);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> setBudget(Long id, BudgetRequest request, Long actorId) {
        requireEmployee(id);
        if (request == null || request.dailyRunLimit() == null || request.dailyRunLimit() < 1
                || request.concurrencyLimit() == null || request.concurrencyLimit() < 1
                || request.concurrencyLimit() > request.dailyRunLimit())
            throw new IllegalArgumentException("每日运行上限与并发上限必须为有效正整数");
        int updated = jdbc.update("""
                UPDATE ea_digital_employee_service SET daily_run_limit=?,concurrency_limit=?,version_no=version_no+1
                WHERE employee_id=? AND version_no=? AND lifecycle_status<>'ARCHIVED'
                """, request.dailyRunLimit(), request.concurrencyLimit(), id, request.versionNo());
        if (updated != 1) throw new IllegalStateException("配置已被其他人修改，请刷新后重试");
        audit(id, actorId, "BUDGET", "每日：" + request.dailyRunLimit() + "，并发：" + request.concurrencyLimit());
        return detail(id);
    }

    @Transactional
    public Map<String, Object> assign(Long id, AssignmentRequest request, Long actorId) {
        requireEmployee(id);
        if (request == null) throw new IllegalArgumentException("缺少使用者分配配置");
        List<Long> users = request.userIds() == null ? List.of() : request.userIds();
        if (new HashSet<>(users).size() != users.size() || users.contains(null))
            throw new IllegalArgumentException("使用者不能重复或为空");
        for (Long userId : users) requireActiveUser(userId);
        bumpVersion(id, request.versionNo());
        jdbc.update("DELETE FROM ea_digital_employee_user WHERE employee_id=?", id);
        for (Long userId : users) jdbc.update("""
                INSERT INTO ea_digital_employee_user (employee_id,user_id,assigned_by) VALUES (?,?,?)
                """, id, userId, actorId);
        audit(id, actorId, "ASSIGN", "使用者数量：" + users.size());
        return detail(id);
    }

    @Transactional
    public Map<String, Object> bind(Long id, BindingRequest request, Long actorId) {
        requireEmployee(id);
        if (request == null || request.shareId() == null || request.permissionCode() == null)
            throw new IllegalArgumentException("缺少共享版本或查询能力");
        List<Map<String, Object>> shares = jdbc.queryForList("""
                SELECT sh.id,sh.status,sh.agent_id,sh.agent_version_id,sh.permission_code,d.status agent_status,
                       v.published,v.config_json
                FROM ea_workflow_share sh JOIN ea_agent_definition d ON d.id=sh.agent_id
                JOIN ea_agent_version v ON v.id=sh.agent_version_id AND v.agent_id=sh.agent_id
                WHERE sh.id=?
                """, request.shareId());
        if (shares.size() != 1 || !"APPROVED".equals(shares.get(0).get("status"))
                || !(shares.get(0).get("published") instanceof Number published) || published.intValue() != 1
                || !"READY".equals(shares.get(0).get("agent_status")))
            throw new IllegalArgumentException("只可绑定已审核、已发布且启用的固定版本");
        String expected = (String) shares.get(0).get("permission_code");
        if (!expected.equals(request.permissionCode())) throw new IllegalArgumentException("工作流与查询能力不匹配");
        List<String> requiredCodes = jdbc.queryForList("""
                SELECT permission_code FROM ea_workflow_share_capability WHERE share_id=?
                """, String.class, request.shareId());
        if (requiredCodes.isEmpty() || !requiredCodes.contains(expected))
            throw new IllegalArgumentException("共享版本缺少查询能力声明");
        Map<String, List<String>> limits = request.companyLimits() == null
                ? Map.of(expected, request.companyLimit() == null ? List.of() : request.companyLimit())
                : request.companyLimits();
        if (requiredCodes.size() > 1 && request.companyLimits() == null)
            throw new IllegalArgumentException("多工具工作流须逐项配置服务范围");
        if (!Set.copyOf(requiredCodes).equals(limits.keySet()))
            throw new IllegalArgumentException("须为固定版本的每项查询能力明确配置服务范围");
        Long responsible = ((Number) requireEmployee(id).get("responsible_user_id")).longValue();
        for (String code : requiredCodes) {
            List<String> selected = limits.getOrDefault(code, List.of());
            Set<String> companies = new HashSet<>(selected);
            if (companies.size() != selected.size()) throw new IllegalArgumentException("公司范围不能重复");
            if (QueryAuthorizationService.PEOPLE_QUERY.equals(code) && !companies.isEmpty())
                throw new IllegalArgumentException("人员查询不使用公司上限");
            for (String company : companies) {
                Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_company WHERE company_id=? AND status=1",
                        Integer.class, company);
                if (count == null || count != 1) throw new IllegalArgumentException("无效的公司范围");
            }
            Set<String> responsibleRange = authorizationService.resolveForSelf(responsible, code).companyIds();
            if (!responsibleRange.containsAll(companies)) throw new SecurityException("超出责任人的数据范围");
        }
        bumpVersion(id, request.versionNo());
        jdbc.update("""
                INSERT INTO ea_digital_employee_workflow (employee_id,share_id,permission_code,status)
                VALUES (?,?,?,1) ON DUPLICATE KEY UPDATE permission_code=VALUES(permission_code),status=1
                """, id, request.shareId(), request.permissionCode());
        for (String code : requiredCodes) {
            jdbc.update("DELETE FROM ea_digital_employee_scope_company WHERE employee_id=? AND permission_code=?",
                    id, code);
            for (String company : limits.getOrDefault(code, List.of())) jdbc.update("""
                    INSERT INTO ea_digital_employee_scope_company (employee_id,permission_code,company_id)
                    VALUES (?,?,?)
                    """, id, code, company);
        }
        audit(id, actorId, "BIND", "共享版本：" + request.shareId());
        return detail(id);
    }

    @Transactional
    public void unbind(Long employeeId, Long bindingId, Long versionNo, Long actorId) {
        requireEmployee(employeeId);
        bumpVersion(employeeId, versionNo);
        int count = jdbc.update("UPDATE ea_digital_employee_workflow SET status=0 WHERE id=? AND employee_id=?",
                bindingId, employeeId);
        if (count != 1) throw new IllegalArgumentException("绑定不存在");
        audit(employeeId, actorId, "UNBIND", "绑定：" + bindingId);
    }

    public List<Map<String, Object>> availableFor(Long userId) {
        return jdbc.queryForList("""
                SELECT e.id,e.name,e.responsible_user_id,s.description,s.avatar_code,b.id binding_id,b.permission_code,b.share_id,
                       sh.agent_id,sh.agent_version_id,d.agent_name
                FROM ea_digital_employee_user u
                JOIN ea_digital_employee e ON e.id=u.employee_id AND e.tenant_id=?
                JOIN ea_digital_employee_service s ON s.employee_id=e.id AND s.lifecycle_status='ACTIVE'
                JOIN ea_digital_employee_workflow b ON b.employee_id=e.id AND b.status=1
                JOIN ea_workflow_share sh ON sh.id=b.share_id AND sh.status='APPROVED'
                  AND sh.permission_code=b.permission_code
                JOIN ea_agent_definition d ON d.id=sh.agent_id AND d.status='READY'
                JOIN ea_agent_version v ON v.id=sh.agent_version_id AND v.agent_id=sh.agent_id AND v.published=1
                WHERE u.user_id=? ORDER BY e.name,b.id
                """, tenant(), userId);
    }

    public List<Map<String, Object>> availableAuthorizedFor(Long userId) {
        return availableFor(userId).stream().filter(candidate -> {
            try {
                Long employeeId = ((Number) candidate.get("id")).longValue();
                List<String> codes = jdbc.queryForList("""
                        SELECT permission_code FROM ea_workflow_share_capability WHERE share_id=?
                        """, String.class, candidate.get("share_id"));
                if (codes.isEmpty()) return false;
                for (String code : codes) {
                    List<String> limit = companyLimit(employeeId, code);
                    authorizationService.resolve(userId,
                            ((Number) candidate.get("responsible_user_id")).longValue(),
                            code, limit.isEmpty() ? null : Set.copyOf(limit));
                }
                return true;
            } catch (SecurityException | IllegalArgumentException exception) {
                return false;
            }
        }).toList();
    }

    public List<String> companyLimit(Long employeeId, String permissionCode) {
        return jdbc.queryForList("""
                SELECT company_id FROM ea_digital_employee_scope_company
                WHERE employee_id=? AND permission_code=?
                """, String.class, employeeId, permissionCode);
    }

    public Map<String, Object> preview(Long employeeId, Long requesterId, String permissionCode) {
        Map<String, Object> employee = requireEmployee(employeeId);
        Integer assigned = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ea_digital_employee_user WHERE employee_id=? AND user_id=?
                """, Integer.class, employeeId, requesterId);
        Integer bound = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ea_digital_employee_workflow b
                JOIN ea_workflow_share sh ON sh.id=b.share_id AND sh.status='APPROVED'
                  AND sh.permission_code=b.permission_code
                JOIN ea_workflow_share_capability sc ON sc.share_id=sh.id AND sc.permission_code=?
                JOIN ea_agent_definition d ON d.id=sh.agent_id AND d.status='READY'
                JOIN ea_agent_version v ON v.id=sh.agent_version_id AND v.agent_id=sh.agent_id AND v.published=1
                WHERE b.employee_id=? AND b.status=1
                """, Integer.class, permissionCode, employeeId);
        if (assigned == null || assigned == 0 || bound == null || bound == 0)
            return Map.of("authorized", false, "reason", "未分配使用者或没有已审核绑定");
        try {
            List<String> limit = companyLimit(employeeId, permissionCode);
            var authorization = authorizationService.resolve(requesterId,
                    ((Number) employee.get("responsible_user_id")).longValue(), permissionCode,
                    limit.isEmpty() ? null : Set.copyOf(limit));
            return Map.of("authorized", true, "companyIds", authorization.companyIds().stream().sorted().toList(),
                    "companyCount", authorization.companyIds().size());
        } catch (SecurityException exception) {
            return Map.of("authorized", false, "reason", "使用者或责任人缺少该查询能力的数据范围");
        }
    }

    public List<Map<String, Object>> audit(Long id) {
        requireEmployee(id);
        return jdbc.queryForList("""
                SELECT action_type,decision_reason,created_at,trace_id,responsible_user_id
                FROM ea_digital_employee_action_log WHERE employee_id=? AND tenant_id=?
                ORDER BY created_at DESC,id DESC LIMIT 100
                """, id, tenant());
    }

    public void audit(Long id, Long actorId, String action, String detail) {
        jdbc.update("""
                INSERT INTO ea_digital_employee_action_log
                (tenant_id,employee_id,responsible_user_id,action_type,decision_reason)
                VALUES (?,?,?,?,?)
                """, tenant(), id, actorId, action, detail);
    }

    private Map<String, Object> requireEmployee(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT e.id,e.name,e.company_id,e.responsible_user_id,s.description,s.avatar_code,
                       s.lifecycle_status,s.daily_run_limit,s.concurrency_limit,s.version_no
                FROM ea_digital_employee e JOIN ea_digital_employee_service s ON s.employee_id=e.id
                WHERE e.id=? AND e.tenant_id=?
                """, id, tenant());
        if (rows.size() != 1) throw new IllegalArgumentException("数字员工不存在");
        return rows.get(0);
    }

    private String userCompany(Long userId) {
        List<String> rows = jdbc.queryForList("""
                SELECT u.company_id FROM sys_user u JOIN sys_company c ON c.company_id=u.company_id AND c.status=1
                WHERE u.id=? AND u.status=1
                """, String.class, userId);
        if (rows.size() != 1) throw new IllegalArgumentException("当前用户没有有效管理归属公司");
        return rows.get(0);
    }

    private void requireActiveUser(Long userId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE id=? AND status=1",
                Integer.class, userId);
        if (count == null || count != 1) throw new IllegalArgumentException("责任人或使用者不存在或已停用");
    }

    private void validateSave(SaveRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()
                || request.name().length() > 100 || request.responsibleUserId() == null)
            throw new IllegalArgumentException("请填写名称和有效责任人");
        if (request.description() != null && request.description().length() > 500)
            throw new IllegalArgumentException("说明不能超过 500 字");
    }

    private void bumpVersion(Long id, Long expectedVersion) {
        if (expectedVersion == null) throw new IllegalArgumentException("缺少配置版本号");
        int updated = jdbc.update("""
                UPDATE ea_digital_employee_service SET version_no=version_no+1
                WHERE employee_id=? AND version_no=? AND lifecycle_status<>'ARCHIVED'
                """, id, expectedVersion);
        if (updated != 1) throw new IllegalStateException("配置已被其他人修改，请刷新后重试");
    }

    private String tenant() { return TenantContext.getEffectiveTenantId(); }
}
