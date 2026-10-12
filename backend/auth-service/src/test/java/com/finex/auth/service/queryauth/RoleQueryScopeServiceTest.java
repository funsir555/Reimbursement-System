package com.finex.auth.service.queryauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.service.AccessControlService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RoleQueryScopeServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final AccessControlService accessControl = mock(AccessControlService.class);
    private final QueryAuthorizationService authorization = mock(QueryAuthorizationService.class);
    private final RoleQueryScopeService service = new RoleQueryScopeService(
            jdbc, new ObjectMapper(), accessControl, authorization);

    @Test
    void uppercaseSuperAdminIsReadOnlyAndDisplaysAllCompanies() {
        when(jdbc.queryForList(contains("FROM sys_role WHERE"), eq(7L)))
                .thenReturn(List.of(Map.of("id", 7L, "role_code", "SUPER_ADMIN", "status", 1)));
        when(jdbc.queryForList(contains("FROM sys_role_permission rp"), eq(7L)))
                .thenReturn(List.of(Map.of("permission_code", QueryAuthorizationService.FINANCE_CLOSE_QUERY,
                        "capability_name", "期间查询", "supported_scopes", "SELECTED_COMPANIES,ALL_COMPANIES")));

        assertThat(service.read(7L)).singleElement().satisfies(scope ->
                assertThat(scope.scopeType()).isEqualTo("ALL_COMPANIES"));
        assertThatThrownBy(() -> service.replace(7L, new RoleQueryScopeService.ScopeRequest(List.of()), 1L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("超级管理员数据范围由系统固定");
    }

    @Test
    void uppercaseSuperAdminMayDelegateRoles() {
        when(accessControl.getRoleCodes(1L)).thenReturn(List.of("SUPER_ADMIN"));

        service.assertCanDelegateRoles(1L, List.of(7L));

        verifyNoInteractions(jdbc);
    }
}
