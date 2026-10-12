package com.finex.auth.support.archiveagent;

import com.finex.auth.agent.context.AgentExecutionContext;
import com.finex.auth.entity.ArchiveAgentRun;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveAgentExecutionContextResolverTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final ArchiveAgentExecutionContextResolver resolver =
            new ArchiveAgentExecutionContextResolver(userMapper);

    @Test
    void keepsTrustedRequestSnapshotForManualRun() {
        ArchiveAgentRun run = run(41L, 7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L, "GROUP_HQ"));
        AgentExecutionContext captured = AgentExecutionContext.builder()
                .tenantId("default")
                .companyId(" GROUP_HQ ")
                .userId(7L)
                .traceId("trace-manual")
                .build();

        AgentExecutionContext resolved = resolver.resolve(run, captured);

        assertThat(resolved.getTenantId()).isEqualTo("default");
        assertThat(resolved.getCompanyId()).isEqualTo("GROUP_HQ");
        assertThat(resolved.getUserId()).isEqualTo(7L);
        assertThat(resolved.getTraceId()).isEqualTo("trace-manual");
        assertThat(resolved.getAgentRunId()).isEqualTo(41L);
        assertThat(resolved.getTriggerSource()).isEqualTo("WORKBENCH");
        verify(userMapper).selectById(7L);
    }

    @Test
    void resolvesScheduledRunCompanyFromCurrentOwner() {
        User owner = activeUser(7L, "GROUP_HQ");
        when(userMapper.selectById(7L)).thenReturn(owner);

        AgentExecutionContext resolved = resolver.resolve(run(42L, 7L), new AgentExecutionContext());

        assertThat(resolved.getTenantId()).isEqualTo("default");
        assertThat(resolved.getCompanyId()).isEqualTo("GROUP_HQ");
        assertThat(resolved.getUserId()).isEqualTo(7L);
        assertThat(resolved.getTraceId()).startsWith("trace_");
    }

    @Test
    void rejectsSnapshotOwnedByAnotherUser() {
        AgentExecutionContext captured = AgentExecutionContext.builder()
                .tenantId("default")
                .companyId("GROUP_HQ")
                .userId(8L)
                .build();

        assertThatThrownBy(() -> resolver.resolve(run(43L, 7L), captured))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("提交身份不一致");
    }

    @Test
    void rejectsManualSnapshotWithoutCompany() {
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L, "GROUP_HQ"));
        AgentExecutionContext captured = AgentExecutionContext.builder()
                .tenantId("default")
                .userId(7L)
                .build();

        assertThatThrownBy(() -> resolver.resolve(run(43L, 7L), captured))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("提交身份缺少可用的公司上下文");
    }

    @Test
    void rejectsInactiveOrCompanylessOwner() {
        User inactive = activeUser(7L, "GROUP_HQ");
        inactive.setStatus(0);
        when(userMapper.selectById(7L)).thenReturn(inactive);

        assertThatThrownBy(() -> resolver.resolve(run(44L, 7L), new AgentExecutionContext()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("不存在或已停用");

        User companyless = activeUser(7L, " ");
        when(userMapper.selectById(7L)).thenReturn(companyless);

        assertThatThrownBy(() -> resolver.resolve(run(44L, 7L), new AgentExecutionContext()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("缺少可用的公司上下文");
    }

    private ArchiveAgentRun run(Long id, Long ownerUserId) {
        ArchiveAgentRun run = new ArchiveAgentRun();
        run.setId(id);
        run.setOwnerUserId(ownerUserId);
        run.setTriggerSource("WORKBENCH");
        return run;
    }

    private User activeUser(Long id, String companyId) {
        User user = new User();
        user.setId(id);
        user.setStatus(1);
        user.setCompanyId(companyId);
        return user;
    }
}
