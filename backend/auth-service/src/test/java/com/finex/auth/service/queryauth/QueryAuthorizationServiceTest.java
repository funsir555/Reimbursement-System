package com.finex.auth.service.queryauth;

import com.finex.auth.agent.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryAuthorizationServiceTest {
    private static final String CODE = QueryAuthorizationService.FINANCE_CLOSE_QUERY;
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final QueryAuthorizationService service = new QueryAuthorizationService(jdbc);

    @AfterEach void clearContext() { TenantContext.clear(); }

    @Test
    void rejectsPermissionWithoutScopeOnTheSameRole() {
        seedCommon();
        when(jdbc.queryForList(contains("FROM sys_user_role"), eq(1L), eq(CODE)))
                .thenReturn(List.of(Map.of("id", 10L, "role_code", "reader")));
        when(jdbc.queryForList(contains("FROM sys_user_role"), eq(2L), eq(CODE)))
                .thenReturn(List.of(Map.of("id", 20L, "role_code", "owner")));
        when(jdbc.queryForList(contains("FROM sys_role_query_scope"), eq(10L), eq(CODE)))
                .thenReturn(List.of());
        when(jdbc.queryForList(contains("FROM sys_role_query_scope"), eq(20L), eq(CODE)))
                .thenReturn(List.of(Map.of("id", 200L, "scope_type", "SELECTED_COMPANIES")));
        when(jdbc.queryForList(contains("FROM sys_role_query_scope_company"), eq(String.class), eq(200L)))
                .thenReturn(List.of("A"));

        assertThatThrownBy(() -> service.resolve(1L, 2L, CODE, null))
                .isInstanceOf(SecurityException.class)
                .hasMessage(QueryAuthorizationService.NO_PERMISSION);
    }

    @Test
    void intersectsTwoUsersAndEmployeeLimitAcrossCompanies() {
        seedCommon();
        when(jdbc.queryForList(contains("FROM sys_user_role"), eq(1L), eq(CODE)))
                .thenReturn(List.of(Map.of("id", 10L, "role_code", "reader")));
        when(jdbc.queryForList(contains("FROM sys_user_role"), eq(2L), eq(CODE)))
                .thenReturn(List.of(Map.of("id", 20L, "role_code", "owner")));
        when(jdbc.queryForList(contains("FROM sys_role_query_scope"), eq(10L), eq(CODE)))
                .thenReturn(List.of(Map.of("id", 100L, "scope_type", "SELECTED_COMPANIES")));
        when(jdbc.queryForList(contains("FROM sys_role_query_scope"), eq(20L), eq(CODE)))
                .thenReturn(List.of(Map.of("id", 200L, "scope_type", "SELECTED_COMPANIES")));
        when(jdbc.queryForList(contains("FROM sys_role_query_scope_company"), eq(String.class), eq(100L)))
                .thenReturn(List.of("A", "B"));
        when(jdbc.queryForList(contains("FROM sys_role_query_scope_company"), eq(String.class), eq(200L)))
                .thenReturn(List.of("B", "C"));

        assertThat(service.resolve(1L, 2L, CODE, Set.of("B", "C")).companyIds())
                .containsExactly("B");
    }

    @Test
    void existingUppercaseSuperAdminRoleGetsAllActiveCompaniesWithoutStoredScope() {
        seedCommon();
        when(jdbc.queryForList(contains("FROM sys_user_role"), eq(1L), eq(CODE)))
                .thenReturn(List.of(Map.of("id", 10L, "role_code", "SUPER_ADMIN")));

        assertThat(service.resolveForSelf(1L, CODE).companyIds())
                .containsExactlyInAnyOrder("A", "B", "C");
    }

    @Test
    void selfScopeFollowsOwnDocumentAcrossPaymentCompaniesButKeepsResponsibleLimit() {
        seedCommon();
        when(jdbc.queryForList(contains("FROM sys_user_role"), eq(1L), eq(CODE)))
                .thenReturn(List.of(Map.of("id", 10L, "role_code", "reader")));
        when(jdbc.queryForList(contains("FROM sys_user_role"), eq(2L), eq(CODE)))
                .thenReturn(List.of(Map.of("id", 20L, "role_code", "owner")));
        when(jdbc.queryForList(contains("FROM sys_role_query_scope"), eq(10L), eq(CODE)))
                .thenReturn(List.of(Map.of("id", 100L, "scope_type", "SELF")));
        when(jdbc.queryForList(contains("FROM sys_role_query_scope"), eq(20L), eq(CODE)))
                .thenReturn(List.of(Map.of("id", 200L, "scope_type", "SELECTED_COMPANIES")));
        when(jdbc.queryForList(contains("FROM sys_role_query_scope_company"), eq(String.class), eq(200L)))
                .thenReturn(List.of("B"));

        QueryAuthorization authorization = service.resolve(1L, 2L, CODE, null);
        assertThat(authorization.companyIds()).containsExactly("B");
        assertThat(authorization.allows("B", 1L)).isTrue();
        assertThat(authorization.allows("B", 3L)).isFalse();
        assertThat(authorization.allows("A", 1L)).isFalse();
    }

    @Test
    void departmentScopeKeepsItsPeopleButAllowsTheirDocumentsInPaymentCompany() {
        seedCommon();
        when(jdbc.queryForList(contains("FROM sys_user_role"), eq(1L), eq(CODE)))
                .thenReturn(List.of(Map.of("id", 10L, "role_code", "reader")));
        when(jdbc.queryForList(contains("FROM sys_user_role"), eq(2L), eq(CODE)))
                .thenReturn(List.of(Map.of("id", 20L, "role_code", "owner")));
        when(jdbc.queryForList(contains("FROM sys_role_query_scope"), eq(10L), eq(CODE)))
                .thenReturn(List.of(Map.of("id", 100L, "scope_type", "DEPARTMENT_TREE")));
        when(jdbc.queryForList(contains("FROM sys_role_query_scope"), eq(20L), eq(CODE)))
                .thenReturn(List.of(Map.of("id", 200L, "scope_type", "SELECTED_COMPANIES")));
        when(jdbc.queryForList(contains("FROM sys_role_query_scope_company"), eq(String.class), eq(200L)))
                .thenReturn(List.of("B"));
        when(jdbc.queryForList(contains("SELECT company_id, dept_id FROM sys_user"), eq(1L)))
                .thenReturn(List.of(Map.of("company_id", "A", "dept_id", 7L)));
        when(jdbc.queryForList("SELECT id, parent_id FROM sys_department WHERE company_id=? AND status=1", "A"))
                .thenReturn(List.of(Map.of("id", 7L)));
        when(jdbc.queryForList(contains("SELECT id FROM sys_user WHERE status=1 AND company_id=?"),
                eq(Long.class), eq("A"), eq(7L))).thenReturn(List.of(3L));

        QueryAuthorization authorization = service.resolve(1L, 2L, CODE, null);
        assertThat(authorization.allows("B", 3L)).isTrue();
        assertThat(authorization.allows("B", 4L)).isFalse();
        assertThat(authorization.allows("A", 3L)).isFalse();
    }

    @Test
    void doesNotUseGlobalCompanyTablesForUnknownTenant() {
        TenantContext.setTenantId("another-tenant");
        assertThatThrownBy(() -> service.resolveForSelf(1L, CODE))
                .isInstanceOf(SecurityException.class)
                .hasMessage(QueryAuthorizationService.NO_PERMISSION);
    }

    private void seedCommon() {
        when(jdbc.queryForObject(contains("FROM sys_user WHERE id=?"), eq(Integer.class), anyLong()))
                .thenReturn(1);
        when(jdbc.queryForList(contains("FROM sys_query_capability"), eq(String.class), eq(CODE)))
                .thenReturn(List.of(CODE));
        when(jdbc.queryForList("SELECT company_id FROM sys_company WHERE status=1", String.class))
                .thenReturn(List.of("A", "B", "C"));
    }
}
