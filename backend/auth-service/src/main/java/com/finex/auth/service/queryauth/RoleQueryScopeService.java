package com.finex.auth.service.queryauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.service.AccessControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Separate role data-scope settings; menu permission changes cannot save these by accident. */
@Service
@RequiredArgsConstructor
public class RoleQueryScopeService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final AccessControlService accessControlService;
    private final QueryAuthorizationService authorizationService;

    public record ScopeInput(String permissionCode, String scopeType, List<String> companyIds) { }
    public record ScopeRequest(List<ScopeInput> scopes) { }
    public record ScopeView(String permissionCode, String capabilityName, List<String> supportedScopes,
                            String scopeType, List<String> companyIds) { }

    public List<ScopeView> read(Long roleId) {
        Map<String, Object> role = requireRole(roleId);
        boolean superAdmin = QueryRoleCodes.isSuperAdmin(role.get("role_code"));
        List<Map<String, Object>> capabilities = jdbc.queryForList("""
                SELECT c.permission_code, c.capability_name, c.supported_scopes
                FROM sys_role_permission rp
                JOIN sys_permission p ON p.id=rp.permission_id AND p.status=1
                JOIN sys_query_capability c ON c.permission_code=p.permission_code AND c.status=1
                WHERE rp.role_id=? ORDER BY c.permission_code
                """, roleId);
        List<ScopeView> result = new ArrayList<>();
        for (Map<String, Object> capability : capabilities) {
            String code = (String) capability.get("permission_code");
            List<String> supported = List.of(((String) capability.get("supported_scopes")).split(","));
            if (superAdmin) {
                result.add(new ScopeView(code, (String) capability.get("capability_name"),
                        supported, "ALL_COMPANIES", List.of()));
                continue;
            }
            List<Map<String, Object>> saved = jdbc.queryForList(
                    "SELECT id, scope_type FROM sys_role_query_scope WHERE role_id=? AND permission_code=?", roleId, code);
            if (saved.isEmpty()) {
                result.add(new ScopeView(code, (String) capability.get("capability_name"), supported, null, List.of()));
            } else {
                Long scopeId = ((Number) saved.get(0).get("id")).longValue();
                result.add(new ScopeView(code, (String) capability.get("capability_name"), supported,
                        (String) saved.get(0).get("scope_type"), jdbc.queryForList(
                                "SELECT company_id FROM sys_role_query_scope_company WHERE scope_id=? ORDER BY company_id",
                                String.class, scopeId)));
            }
        }
        return result;
    }

    @Transactional
    public List<ScopeView> replace(Long roleId, ScopeRequest request, Long actorUserId) {
        Map<String, Object> role = requireRole(roleId);
        if (QueryRoleCodes.isSuperAdmin(role.get("role_code"))) {
            throw new SecurityException("超级管理员数据范围由系统固定，不能修改");
        }
        List<ScopeInput> inputs = request == null || request.scopes() == null ? List.of() : request.scopes();
        Set<String> allowed = new HashSet<>();
        for (ScopeView view : read(roleId)) allowed.add(view.permissionCode());
        Set<String> seen = new HashSet<>();
        boolean actorIsSuperAdmin = QueryRoleCodes.hasSuperAdmin(accessControlService.getRoleCodes(actorUserId));
        for (ScopeInput input : inputs) {
            if (input == null || !allowed.contains(input.permissionCode()) || !seen.add(input.permissionCode())) {
                throw new IllegalArgumentException("查询权限码未授予、重复或无效");
            }
            String supported = jdbc.queryForObject(
                    "SELECT supported_scopes FROM sys_query_capability WHERE permission_code=? AND status=1",
                    String.class, input.permissionCode());
            if (input.scopeType() == null || !List.of(supported.split(",")).contains(input.scopeType())) {
                throw new IllegalArgumentException("该查询能力不支持所选数据范围");
            }
            List<String> companies = input.companyIds() == null ? List.of() : input.companyIds();
            if ("SELECTED_COMPANIES".equals(input.scopeType())) {
                if (companies.isEmpty() || companies.stream().anyMatch(id -> id == null || id.isBlank())
                        || new HashSet<>(companies).size() != companies.size()) {
                    throw new IllegalArgumentException("指定公司范围必须选择不重复的有效公司");
                }
                String placeholders = String.join(",", java.util.Collections.nCopies(companies.size(), "?"));
                Integer count = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM sys_company WHERE status=1 AND company_id IN (" + placeholders + ")",
                        Integer.class, companies.toArray());
                if (count == null || count != companies.size()) throw new IllegalArgumentException("包含无效公司");
                if (!actorIsSuperAdmin) {
                    Set<String> delegable = authorizationService.resolveForSelf(actorUserId,
                            input.permissionCode()).requesterGrants().stream()
                            .filter(grant -> grant.subjectUserIds() == null)
                            .flatMap(grant -> grant.companyIds().stream())
                            .collect(java.util.stream.Collectors.toSet());
                    if (!delegable.containsAll(companies)) throw new SecurityException("超出可委派的数据范围");
                }
            } else {
                if (!companies.isEmpty()) throw new IllegalArgumentException("此范围不能携带公司列表");
                // SELF/DEPARTMENT_TREE vary with the recipient. Only a super admin can prove safe delegation.
                if (!actorIsSuperAdmin) throw new SecurityException("此动态范围仅超级管理员可配置");
            }
            if ("ALL_COMPANIES".equals(input.scopeType()) && !actorIsSuperAdmin) {
                throw new SecurityException("仅超级管理员可配置全部公司");
            }
        }
        String before = json(read(roleId));
        deleteScopeRows(roleId);
        for (ScopeInput input : inputs) {
            jdbc.update("INSERT INTO sys_role_query_scope (role_id,permission_code,scope_type,updated_by) VALUES (?,?,?,?)",
                    roleId, input.permissionCode(), input.scopeType(), actorUserId);
            Long scopeId = jdbc.queryForObject("SELECT id FROM sys_role_query_scope WHERE role_id=? AND permission_code=?",
                    Long.class, roleId, input.permissionCode());
            for (String company : input.companyIds() == null ? List.<String>of() : input.companyIds()) {
                jdbc.update("INSERT INTO sys_role_query_scope_company (scope_id,company_id) VALUES (?,?)", scopeId, company);
            }
        }
        List<ScopeView> after = read(roleId);
        jdbc.update("INSERT INTO sys_query_scope_audit (role_id,actor_user_id,before_json,after_json) VALUES (?,?,?,?)",
                roleId, actorUserId, before, json(after));
        return after;
    }

    @Transactional
    public void pruneRevoked(Long roleId) {
        List<Long> stale = jdbc.queryForList("""
                SELECT s.id FROM sys_role_query_scope s
                LEFT JOIN sys_permission p ON p.permission_code=s.permission_code AND p.status=1
                LEFT JOIN sys_role_permission rp ON rp.role_id=s.role_id AND rp.permission_id=p.id
                WHERE s.role_id=? AND rp.id IS NULL
                """, Long.class, roleId);
        for (Long id : stale) {
            jdbc.update("DELETE FROM sys_role_query_scope_company WHERE scope_id=?", id);
            jdbc.update("DELETE FROM sys_role_query_scope WHERE id=?", id);
        }
    }

    public void assertCanDelegateRoles(Long actorUserId, List<Long> roleIds) {
        if (QueryRoleCodes.hasSuperAdmin(accessControlService.getRoleCodes(actorUserId))) return;
        for (Long roleId : roleIds == null ? List.<Long>of() : roleIds) {
            for (ScopeView scope : read(roleId)) {
                if (scope.scopeType() == null) continue;
                if (!"SELECTED_COMPANIES".equals(scope.scopeType()))
                    throw new SecurityException("动态或全部公司范围仅超级管理员可委派");
                Set<String> ownCompanyGrants = authorizationService.resolveForSelf(actorUserId,
                        scope.permissionCode()).requesterGrants().stream()
                        .filter(grant -> grant.subjectUserIds() == null)
                        .flatMap(grant -> grant.companyIds().stream())
                        .collect(java.util.stream.Collectors.toSet());
                if (!ownCompanyGrants.containsAll(scope.companyIds()))
                    throw new SecurityException("角色数据范围超出当前管理员可委派范围");
            }
        }
    }

    private void deleteScopeRows(Long roleId) {
        jdbc.update("DELETE sc FROM sys_role_query_scope_company sc JOIN sys_role_query_scope s ON s.id=sc.scope_id WHERE s.role_id=?", roleId);
        jdbc.update("DELETE FROM sys_role_query_scope WHERE role_id=?", roleId);
    }

    private Map<String, Object> requireRole(Long roleId) {
        List<Map<String, Object>> roles = jdbc.queryForList(
                "SELECT id,role_code,status FROM sys_role WHERE id=?", roleId);
        if (roles.size() != 1 || ((Number) roles.get(0).get("status")).intValue() != 1) {
            throw new IllegalArgumentException("角色不存在或已停用");
        }
        return roles.get(0);
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("无法记录数据范围审计", exception); }
    }
}
