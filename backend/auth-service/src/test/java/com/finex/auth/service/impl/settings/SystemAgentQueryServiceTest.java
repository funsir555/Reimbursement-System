package com.finex.auth.service.impl.settings;

import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SystemAgentQueryServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final SystemAgentQueryService service = new SystemAgentQueryService(jdbc);

    @Test
    void personnelLookupRejectsBorrowedDepartmentPermission() {
        assertThatThrownBy(() -> service.query("PEOPLE", grant("system:agent:departments:query"),
                Map.of("name", "张三"))).isInstanceOf(SecurityException.class);
        verifyNoInteractions(jdbc);
    }

    @Test
    void personnelLookupUsesOnlyFourApprovedColumns() {
        var permission = grant(QueryAuthorizationService.PEOPLE_QUERY);
        when(jdbc.queryForObject(org.mockito.ArgumentMatchers.anyString(), eq(Long.class), eq("%张三%")))
                .thenReturn(1L);
        when(jdbc.queryForList(argThat(sql -> sql.contains(
                "SELECT u.id,u.username,u.position,u.labor_relation_belong")
                && !sql.contains("u.password") && !sql.contains("u.phone")),
                eq("%张三%"), eq(50), eq(0)))
                .thenReturn(List.of(Map.of("id", 1L, "username", "u1",
                        "position", "会计", "labor_relation_belong", "A")));

        var page = service.query("PEOPLE", permission, Map.of("name", "张三"));

        assertThat(page.records()).singleElement().satisfies(item ->
                assertThat(item.keySet()).containsExactlyInAnyOrder(
                        "id", "username", "position", "labor_relation_belong"));
    }

    private QueryAuthorization grant(String code) {
        var scope = new QueryAuthorization.Grant("TENANT_ALL", Set.of("A"), null);
        return new QueryAuthorization(7L, 7L, code, List.of(scope), List.of(scope), null);
    }
}
