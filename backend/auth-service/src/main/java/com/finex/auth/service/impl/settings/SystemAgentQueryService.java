package com.finex.auth.service.impl.settings;

import com.finex.auth.service.port.FinanceAgentQueryPort;
import com.finex.auth.service.port.SystemAgentQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** System-owned, fixed projections for Agent personnel and department lookup. */
@Service
@RequiredArgsConstructor
public class SystemAgentQueryService implements SystemAgentQueryPort {
    private final JdbcTemplate jdbc;

    @Override
    public FinanceAgentQueryPort.Page query(String kind, QueryAuthorization authorization,
                                            Map<String, Object> parameters) {
        String permission = switch (kind) {
            case "PEOPLE" -> QueryAuthorizationService.PEOPLE_QUERY;
            case "DEPARTMENT" -> "system:agent:departments:query";
            default -> throw new IllegalArgumentException("未知系统查询类型");
        };
        if (authorization == null || !permission.equals(authorization.permissionCode()))
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        int page = integer(parameters, "page", 1);
        int size = integer(parameters, "pageSize", 50);
        if (page < 1 || size < 1 || size > 200 || (long) (page - 1) * size > Integer.MAX_VALUE)
            throw new IllegalArgumentException("分页参数无效");
        if ("PEOPLE".equals(kind)) {
            String name = text(parameters, "name");
            if (name == null || name.codePointCount(0, name.length()) < 2)
                throw new IllegalArgumentException("姓名至少填写两个字符");
            String pattern = "%" + name.replace("!", "!!").replace("%", "!%")
                    .replace("_", "!_") + "%";
            String where = "u.status=1 AND u.name LIKE ? ESCAPE '!'"
                    + " AND EXISTS (SELECT 1 FROM sys_company c WHERE c.company_id=u.company_id AND c.status=1)";
            long total = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user u WHERE " + where,
                    Long.class, pattern);
            List<Map<String, Object>> records = jdbc.queryForList("""
                    SELECT u.id,u.username,u.position,u.labor_relation_belong
                    FROM sys_user u WHERE
                    """ + where + " ORDER BY u.name,u.id LIMIT ? OFFSET ?", pattern, size, (page - 1) * size);
            return new FinanceAgentQueryPort.Page(total, page, size, records);
        }
        if (!"DEPARTMENT".equals(kind)) throw new IllegalArgumentException("未知系统查询类型");
        String companyId = text(parameters, "companyId");
        if (companyId == null || !authorization.allows(companyId, null))
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        Integer ready = jdbc.queryForObject("""
                SELECT COUNT(*) FROM fin_account_set a JOIN sys_company c ON c.company_id=a.company_id
                WHERE a.company_id=? AND a.status='READY' AND c.status=1
                """, Integer.class, companyId);
        if (ready == null || ready != 1) throw new IllegalArgumentException("账套不存在或尚未就绪");
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM sys_department WHERE company_id=?",
                Long.class, companyId);
        List<Map<String, Object>> records = jdbc.queryForList("""
                SELECT id,dept_code,dept_name,parent_id,status FROM sys_department
                WHERE company_id=? ORDER BY dept_code,id LIMIT ? OFFSET ?
                """, companyId, size, (page - 1) * size);
        return new FinanceAgentQueryPort.Page(total, page, size, records);
    }

    private String text(Map<String, Object> values, String key) {
        Object value = values == null ? null : values.get(key);
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value).trim();
    }
    private int integer(Map<String, Object> values, String key, int fallback) {
        String value = text(values, key);
        if (value == null) return fallback;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException(key + " 必须为整数"); }
    }
}
