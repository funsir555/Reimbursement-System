package com.finex.auth.support.archiveagent;

import com.finex.auth.agent.context.AgentExecutionContext;
import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.agent.context.TraceContext;
import com.finex.auth.entity.ArchiveAgentRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ArchiveAgentTriggerDispatcherTest {

    @AfterEach
    void clearContext() {
        AgentExecutionContext.clear();
    }

    @Test
    void capturesIdentityBeforeSubmittingAsyncRun() {
        ArchiveAgentExecutionWorker worker = mock(ArchiveAgentExecutionWorker.class);
        Executor directExecutor = Runnable::run;
        ArchiveAgentTriggerDispatcher dispatcher = new ArchiveAgentTriggerDispatcher(worker, directExecutor);
        ArchiveAgentRun run = new ArchiveAgentRun();
        run.setId(51L);
        run.setOwnerUserId(7L);
        run.setTriggerSource("WORKBENCH");

        TenantContext.setTenantId("default");
        TenantContext.setCompanyId("GROUP_HQ");
        TenantContext.setUserId(7L);
        TraceContext.setTraceId("trace-dispatch");

        dispatcher.dispatch(run);

        ArgumentCaptor<AgentExecutionContext> contextCaptor =
                ArgumentCaptor.forClass(AgentExecutionContext.class);
        verify(worker).process(org.mockito.ArgumentMatchers.eq(51L), contextCaptor.capture());
        AgentExecutionContext captured = contextCaptor.getValue();
        assertThat(captured.getTenantId()).isEqualTo("default");
        assertThat(captured.getCompanyId()).isEqualTo("GROUP_HQ");
        assertThat(captured.getUserId()).isEqualTo(7L);
        assertThat(captured.getTraceId()).isEqualTo("trace-dispatch");
        assertThat(captured.getAgentRunId()).isEqualTo(51L);
        assertThat(captured.getTriggerSource()).isEqualTo("WORKBENCH");
    }
}
