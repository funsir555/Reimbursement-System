package com.finex.auth.support.archiveagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.context.AgentExecutionContext;
import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.agent.context.TraceContext;
import com.finex.auth.agent.workflow.AgentWorkflowRuntimeService;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentRun;
import com.finex.auth.entity.ArchiveAgentVersion;
import com.finex.auth.mapper.ArchiveAgentDefinitionMapper;
import com.finex.auth.mapper.ArchiveAgentRunArtifactMapper;
import com.finex.auth.mapper.ArchiveAgentRunMapper;
import com.finex.auth.mapper.ArchiveAgentRunStepMapper;
import com.finex.auth.mapper.ArchiveAgentVersionMapper;
import com.finex.auth.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveAgentExecutionWorkerContextTest {

    private final ArchiveAgentRunMapper runMapper = mock(ArchiveAgentRunMapper.class);
    private final ArchiveAgentRunStepMapper stepMapper = mock(ArchiveAgentRunStepMapper.class);
    private final ArchiveAgentRunArtifactMapper artifactMapper = mock(ArchiveAgentRunArtifactMapper.class);
    private final ArchiveAgentDefinitionMapper definitionMapper = mock(ArchiveAgentDefinitionMapper.class);
    private final ArchiveAgentVersionMapper versionMapper = mock(ArchiveAgentVersionMapper.class);
    private final AgentWorkflowRuntimeService workflowRuntime = mock(AgentWorkflowRuntimeService.class);
    private final ArchiveAgentModelRuntime modelRuntime = mock(ArchiveAgentModelRuntime.class);
    private final ArchiveAgentExecutionContextResolver contextResolver =
            mock(ArchiveAgentExecutionContextResolver.class);

    @AfterEach
    void clearContext() {
        AgentExecutionContext.clear();
    }

    @Test
    void restoresAndClearsIdentityForEachRunOnTheSameThread() {
        ArchiveAgentExecutionWorker worker = worker();
        ArchiveAgentRun firstRun = run(61L, 7L);
        ArchiveAgentRun secondRun = run(62L, 8L);
        when(runMapper.selectById(61L)).thenReturn(firstRun);
        when(runMapper.selectById(62L)).thenReturn(secondRun);
        when(contextResolver.resolve(firstRun, null)).thenReturn(context("COMPANY_A", 7L, "trace-a", 61L));
        when(contextResolver.resolve(secondRun, null)).thenReturn(context("COMPANY_B", 8L, "trace-b", 62L));
        when(definitionMapper.selectById(10L)).thenReturn(definition(10L));
        when(versionMapper.selectById(20L)).thenReturn(version(20L));

        AtomicInteger invocation = new AtomicInteger();
        when(workflowRuntime.isControlledQueryWorkflow(any())).thenAnswer(ignored -> {
            int current = invocation.getAndIncrement();
            assertThat(TenantContext.getCompanyId()).isEqualTo(current == 0 ? "COMPANY_A" : "COMPANY_B");
            assertThat(TenantContext.getUserId()).isEqualTo(current == 0 ? 7L : 8L);
            assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(current == 0 ? 7L : 8L);
            assertThat(TraceContext.currentOrNull()).isEqualTo(current == 0 ? "trace-a" : "trace-b");
            return true;
        });

        worker.process(61L, null);
        assertContextsCleared();
        worker.process(62L, null);
        assertContextsCleared();
        assertThat(invocation.get()).isEqualTo(2);
    }

    @Test
    void marksRunFailedWhenIdentityCannotBeResolved() {
        ArchiveAgentExecutionWorker worker = worker();
        ArchiveAgentRun run = run(63L, 7L);
        when(runMapper.selectById(63L)).thenReturn(run);
        when(contextResolver.resolve(run, null))
                .thenThrow(new SecurityException("Agent 运行用户缺少可用的公司上下文"));

        worker.process(63L, null);

        assertThat(run.getStatus()).isEqualTo(ArchiveAgentSupport.RUN_STATUS_FAILED);
        assertThat(run.getErrorMessage()).contains("缺少可用的公司上下文");
        verify(runMapper).updateById(run);
        assertContextsCleared();
    }

    private ArchiveAgentExecutionWorker worker() {
        return new ArchiveAgentExecutionWorker(
                runMapper,
                stepMapper,
                artifactMapper,
                definitionMapper,
                versionMapper,
                new ObjectMapper(),
                List.of(),
                workflowRuntime,
                modelRuntime,
                contextResolver
        );
    }

    private ArchiveAgentRun run(Long id, Long ownerUserId) {
        ArchiveAgentRun run = new ArchiveAgentRun();
        run.setId(id);
        run.setRunNo("RUN-" + id);
        run.setAgentId(10L);
        run.setAgentVersionId(20L);
        run.setOwnerUserId(ownerUserId);
        return run;
    }

    private ArchiveAgentDefinition definition(Long id) {
        ArchiveAgentDefinition definition = new ArchiveAgentDefinition();
        definition.setId(id);
        definition.setOwnerUserId(7L);
        return definition;
    }

    private ArchiveAgentVersion version(Long id) {
        ArchiveAgentVersion version = new ArchiveAgentVersion();
        version.setId(id);
        version.setConfigJson("{}");
        return version;
    }

    private AgentExecutionContext context(String companyId, Long userId, String traceId, Long runId) {
        return AgentExecutionContext.builder()
                .tenantId("default")
                .companyId(companyId)
                .userId(userId)
                .traceId(traceId)
                .agentRunId(runId)
                .build();
    }

    private void assertContextsCleared() {
        assertThat(TenantContext.getTenantId()).isNull();
        assertThat(TenantContext.getCompanyId()).isNull();
        assertThat(TenantContext.getUserId()).isNull();
        assertThat(SecurityUtils.getCurrentUserId()).isNull();
        assertThat(TraceContext.currentOrNull()).isNull();
    }
}
