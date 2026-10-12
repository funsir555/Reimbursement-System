package com.finex.auth.service.digitalemployee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.entity.AgentMessage;
import com.finex.auth.agent.workflow.ControlledToolResult;
import com.finex.auth.agent.workflow.ToolExecutionRequest;
import com.finex.auth.agent.workflow.ToolRuntimeService;
import com.finex.auth.service.port.ExpenseAgentQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ControlledMessageAccessServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final DigitalEmployeeService employees = mock(DigitalEmployeeService.class);
    private final QueryAuthorizationService authorization = mock(QueryAuthorizationService.class);
    private final ExpenseAgentQueryPort expense = mock(ExpenseAgentQueryPort.class);
    private final ToolRuntimeService tools = mock(ToolRuntimeService.class);
    private final ControlledMessageAccessService access = new ControlledMessageAccessService(
            jdbc, employees, authorization, expense, new ObjectMapper(), tools);

    @Test
    void incompleteCompositeProvenanceCannotExposeHistoricalAnswer() {
        AgentMessage message = new AgentMessage();
        message.setId(31L);
        when(jdbc.queryForList(anyString(), anyLong())).thenReturn(
                List.of(Map.of("origin_count", 2, "item_no", 1)));

        assertThat(access.canRead(9L, message)).isFalse();
        verifyNoInteractions(employees, authorization, expense);
    }

    @Test
    void changedToolSourceCannotExposeHistoricalAnswer() {
        String permission = "finance:agent:account_subjects:query";
        var scope = new QueryAuthorization.Grant("SELECTED_COMPANIES", Set.of("A"), null);
        var grant = new QueryAuthorization(9L, 8L, permission, List.of(scope), List.of(scope), null);
        Map<String, Object> origin = Map.of(
                "tool_code", "finance.query_account_subjects", "permission_code", permission,
                "employee_id", 1L, "binding_id", 2L, "agent_version_id", 3L,
                "responsible_user_id", 8L, "auth_digest", grant.digest(),
                "query_json", "{\"companyId\":\"A\"}", "source_digest", "old");
        when(employees.availableFor(9L)).thenReturn(List.of(Map.of("id", 1L,
                "binding_id", 2L, "agent_version_id", 3L, "share_id", 4L)));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(4L), eq(permission)))
                .thenReturn(1);
        when(employees.detail(1L)).thenReturn(Map.of("responsible_user_id", 8L));
        when(employees.companyLimit(1L, permission)).thenReturn(List.of());
        when(authorization.resolve(9L, 8L, permission, null)).thenReturn(grant);
        when(tools.execute(eq("finance.query_account_subjects"), any(ToolExecutionRequest.class)))
                .thenReturn(ControlledToolResult.builder().success(true).sourceDigest("changed").build());

        assertThat(access.canReadToolOrigin(9L, origin)).isFalse();
    }
}
