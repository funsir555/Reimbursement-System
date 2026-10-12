package com.finex.auth.service.digitalemployee;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** Independent metric dimensions; no composite score hides authorization failures. */
@Service
@RequiredArgsConstructor
public class DigitalEmployeeMetricsService {
    private final JdbcTemplate jdbc;
    private final DigitalEmployeeService employees;

    public Map<String, Object> metrics(Long employeeId, int days) {
        Map<String, Object> employee = employees.detail(employeeId);
        if (days != 7 && days != 30) throw new IllegalArgumentException("仅支持 7 天或 30 天视图");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", days);
        result.put("assignedUsers", count("SELECT COUNT(*) FROM ea_digital_employee_user WHERE employee_id=?", employeeId));
        result.put("subscribers", count("""
                SELECT COUNT(DISTINCT s.user_id) FROM ea_digital_employee_subscription s
                JOIN ea_digital_employee_plan p ON p.id=s.plan_id
                WHERE p.employee_id=? AND s.enabled=1
                """, employeeId));
        String since = "DATE_SUB(NOW(), INTERVAL " + days + " DAY)";
        result.put("successfulRuns", count("SELECT COUNT(*) FROM ea_digital_employee_delivery WHERE employee_id=? AND status='SUCCESS' AND created_at>=" + since, employeeId));
        result.put("zeroResults", count("SELECT COUNT(*) FROM ea_digital_employee_delivery WHERE employee_id=? AND status='ZERO' AND created_at>=" + since, employeeId));
        result.put("permissionDenials", count("SELECT COUNT(*) FROM ea_digital_employee_delivery WHERE employee_id=? AND status='NO_PERMISSION' AND created_at>=" + since, employeeId));
        result.put("executionFailures", count("SELECT COUNT(*) FROM ea_digital_employee_delivery WHERE employee_id=? AND status='EXECUTION_FAILED' AND created_at>=" + since, employeeId));
        result.put("onTimeDeliveries", count("""
                SELECT COUNT(*) FROM ea_digital_employee_delivery WHERE employee_id=?
                  AND status IN ('SUCCESS','ZERO') AND created_at>=%s
                  AND finished_at<=DATE_ADD(scheduled_at, INTERVAL 10 MINUTE)
                """.formatted(since), employeeId));
        result.put("dailyRunLimit", employee.get("daily_run_limit"));
        result.put("concurrencyLimit", employee.get("concurrency_limit"));
        result.put("runsToday", count("SELECT COUNT(*) FROM ea_digital_employee_delivery WHERE employee_id=? AND created_at>=CURRENT_DATE()", employeeId));
        result.put("pausedPlans", count("SELECT COUNT(*) FROM ea_digital_employee_plan WHERE employee_id=? AND status IN ('PAUSED_BUDGET','PAUSED_AUTHORIZATION')", employeeId));
        return result;
    }

    private int count(String sql, Long employeeId) {
        Integer value = jdbc.queryForObject(sql, Integer.class, employeeId);
        return value == null ? 0 : value;
    }
}
