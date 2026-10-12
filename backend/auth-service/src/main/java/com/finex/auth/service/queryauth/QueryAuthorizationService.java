package com.finex.auth.service.queryauth;

import com.finex.auth.agent.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Resolves capability and scope from the same active role, on every use. */
@Service
@RequiredArgsConstructor
public class QueryAuthorizationService {
    public static final String NO_PERMISSION = "系统查询到您没有相关权限，请换一个问题";
    public static final String EXPENSE_QUERY = "expense:agent:query";
    public static final String FINANCE_CLOSE_QUERY = "finance:period_close:query";
    public static final String PEOPLE_QUERY = "system:agent:people:query";

    private final JdbcTemplate jdbc;

    public QueryAuthorization resolve(Long requesterId, Long responsibleId, String permissionCode,
                                      Set<String> serviceCompanyLimit) {
        // sys_company and role tables are currently global to the single default tenant.
        // A non-default tenant must not inherit their company or role grants.
        if (TenantContext.getTenantId() != null && !"default".equals(TenantContext.getTenantId())) {
            throw new SecurityException(NO_PERMISSION);
        }
        if (requesterId == null || responsibleId == null || permissionCode == null) {
            throw new SecurityException(NO_PERMISSION);
        }
        if (!activeUser(requesterId) || !activeUser(responsibleId)) throw new SecurityException(NO_PERMISSION);
        List<String> capabilities = jdbc.queryForList(
                "SELECT permission_code FROM sys_query_capability WHERE permission_code=? AND status=1",
                String.class, permissionCode);
        if (capabilities.isEmpty()) {
            throw new SecurityException(NO_PERMISSION);
        }
        List<QueryAuthorization.Grant> requester = grants(requesterId, permissionCode);
        List<QueryAuthorization.Grant> responsible = grants(responsibleId, permissionCode);
        QueryAuthorization authorization = new QueryAuthorization(requesterId, responsibleId,
                permissionCode, requester, responsible, serviceCompanyLimit);
        if (authorization.isEmpty()) {
            throw new SecurityException(NO_PERMISSION);
        }
        return authorization;
    }

    public QueryAuthorization resolveForSelf(Long userId, String permissionCode) {
        return resolve(userId, userId, permissionCode, null);
    }

    private List<QueryAuthorization.Grant> grants(Long userId, String permissionCode) {
        List<Map<String, Object>> roles = jdbc.queryForList("""
                SELECT r.id, r.role_code FROM sys_user_role ur
                JOIN sys_role r ON r.id=ur.role_id AND r.status=1
                JOIN sys_role_permission rp ON rp.role_id=r.id
                JOIN sys_permission p ON p.id=rp.permission_id AND p.status=1
                WHERE ur.user_id=? AND p.permission_code=?
                """, userId, permissionCode);
        if (roles.isEmpty()) return List.of();

        List<String> activeCompanies = activeCompanies();
        List<QueryAuthorization.Grant> result = new ArrayList<>();
        for (Map<String, Object> role : roles) {
            if (PEOPLE_QUERY.equals(permissionCode)) {
                if (!activeCompanies.isEmpty()) result.add(new QueryAuthorization.Grant(
                        "TENANT_ALL", Set.copyOf(activeCompanies), null));
                continue;
            }
            if (QueryRoleCodes.isSuperAdmin(role.get("role_code"))) {
                result.add(new QueryAuthorization.Grant("ALL_COMPANIES", Set.copyOf(activeCompanies), null));
                continue;
            }
            Long roleId = ((Number) role.get("id")).longValue();
            List<Map<String, Object>> scopeRows = jdbc.queryForList("""
                    SELECT s.id, s.scope_type FROM sys_role_query_scope s
                    JOIN sys_query_capability c ON c.permission_code=s.permission_code AND c.status=1
                    WHERE s.role_id=? AND s.permission_code=?
                      AND FIND_IN_SET(s.scope_type, c.supported_scopes)>0
                    """, roleId, permissionCode);
            if (scopeRows.size() != 1) continue;
            Map<String, Object> scope = scopeRows.get(0);
            String scopeType = (String) scope.get("scope_type");
            switch (scopeType) {
                case "SELF" -> {
                    // The personal relation, not the submitter's current company, limits this grant.
                    if (!activeCompanies.isEmpty()) result.add(new QueryAuthorization.Grant(
                            scopeType, Set.copyOf(activeCompanies), Set.of(userId)));
                }
                case "DEPARTMENT_TREE" -> {
                    List<Map<String, Object>> users = jdbc.queryForList(
                            "SELECT company_id, dept_id FROM sys_user WHERE id=? AND status=1", userId);
                    if (users.isEmpty() || users.get(0).get("dept_id") == null) continue;
                    String companyId = (String) users.get(0).get("company_id");
                    if (!activeCompanies.contains(companyId)) continue;
                    Set<Long> departments = descendantDepartments(companyId,
                            ((Number) users.get(0).get("dept_id")).longValue());
                    if (departments.isEmpty()) continue;
                    String placeholders = departments.stream().map(id -> "?").collect(Collectors.joining(","));
                    List<Object> args = new ArrayList<>();
                    args.add(companyId);
                    args.addAll(departments);
                    Set<Long> subjects = Set.copyOf(jdbc.queryForList(
                            "SELECT id FROM sys_user WHERE status=1 AND company_id=? AND dept_id IN (" + placeholders + ")",
                            Long.class, args.toArray()));
                    // Departments identify subjects in the user's organization. Their documents
                    // can belong to another active company when that company pays the expense.
                    result.add(new QueryAuthorization.Grant(scopeType, Set.copyOf(activeCompanies), subjects));
                }
                case "SELECTED_COMPANIES" -> {
                    Long scopeId = ((Number) scope.get("id")).longValue();
                    Set<String> companyIds = jdbc.queryForList("""
                            SELECT c.company_id FROM sys_role_query_scope_company sc
                            JOIN sys_company c ON c.company_id=sc.company_id AND c.status=1
                            WHERE sc.scope_id=?
                            """, String.class, scopeId).stream().collect(Collectors.toSet());
                    if (!companyIds.isEmpty()) result.add(new QueryAuthorization.Grant(scopeType, companyIds, null));
                }
                case "ALL_COMPANIES" -> result.add(new QueryAuthorization.Grant(
                        scopeType, Set.copyOf(activeCompanies), null));
                default -> { /* Unknown scope fails closed. */ }
            }
        }
        return result;
    }

    private Set<Long> descendantDepartments(String companyId, Long rootId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, parent_id FROM sys_department WHERE company_id=? AND status=1", companyId);
        Map<Long, List<Long>> children = rows.stream().filter(row -> row.get("parent_id") != null)
                .collect(Collectors.groupingBy(row -> ((Number) row.get("parent_id")).longValue(),
                        Collectors.mapping(row -> ((Number) row.get("id")).longValue(), Collectors.toList())));
        Set<Long> valid = rows.stream().map(row -> ((Number) row.get("id")).longValue()).collect(Collectors.toSet());
        if (!valid.contains(rootId)) return Set.of();
        Set<Long> result = new HashSet<>();
        ArrayDeque<Long> pending = new ArrayDeque<>();
        pending.add(rootId);
        while (!pending.isEmpty()) {
            Long current = pending.removeFirst();
            if (result.add(current)) pending.addAll(children.getOrDefault(current, List.of()));
        }
        return result;
    }

    private List<String> activeCompanies() {
        return jdbc.queryForList("SELECT company_id FROM sys_company WHERE status=1", String.class);
    }

    private boolean activeUser(Long userId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE id=? AND status=1",
                Integer.class, userId);
        return count != null && count == 1;
    }
}
