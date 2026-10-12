package com.finex.auth.service.impl.archiveagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ArchiveAgentRunDetailVO;
import com.finex.auth.dto.ArchiveAgentRunDTO;
import com.finex.auth.dto.ArchiveAgentRunVO;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentRun;
import com.finex.auth.entity.ArchiveAgentRunArtifact;
import com.finex.auth.entity.ArchiveAgentRunStep;
import com.finex.auth.entity.ArchiveAgentVersion;
import com.finex.auth.mapper.ArchiveAgentDefinitionMapper;
import com.finex.auth.mapper.ArchiveAgentRunArtifactMapper;
import com.finex.auth.mapper.ArchiveAgentRunMapper;
import com.finex.auth.mapper.ArchiveAgentRunStepMapper;
import com.finex.auth.mapper.ArchiveAgentScheduleMapper;
import com.finex.auth.mapper.ArchiveAgentToolBindingMapper;
import com.finex.auth.mapper.ArchiveAgentTriggerMapper;
import com.finex.auth.mapper.ArchiveAgentVersionMapper;
import com.finex.auth.support.archiveagent.ArchiveAgentSupport;
import com.finex.auth.support.archiveagent.TriggerDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveAgentRunDomainSupportTest {

    @Mock private ArchiveAgentDefinitionMapper archiveAgentDefinitionMapper;
    @Mock private ArchiveAgentVersionMapper archiveAgentVersionMapper;
    @Mock private ArchiveAgentTriggerMapper archiveAgentTriggerMapper;
    @Mock private ArchiveAgentToolBindingMapper archiveAgentToolBindingMapper;
    @Mock private ArchiveAgentRunMapper archiveAgentRunMapper;
    @Mock private ArchiveAgentRunStepMapper archiveAgentRunStepMapper;
    @Mock private ArchiveAgentRunArtifactMapper archiveAgentRunArtifactMapper;
    @Mock private ArchiveAgentScheduleMapper archiveAgentScheduleMapper;
    @Mock private TriggerDispatcher triggerDispatcher;

    private ArchiveAgentRunDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new ArchiveAgentRunDomainSupport(AbstractArchiveAgentSupport.dependencies(
                archiveAgentDefinitionMapper,
                archiveAgentVersionMapper,
                archiveAgentTriggerMapper,
                archiveAgentToolBindingMapper,
                archiveAgentRunMapper,
                archiveAgentRunStepMapper,
                archiveAgentRunArtifactMapper,
                archiveAgentScheduleMapper,
                new ObjectMapper(),
                triggerDispatcher
        ));
    }

    @Test
    void runAgentCreatesPendingRunAndDispatches() {
        ArchiveAgentDefinition definition = new ArchiveAgentDefinition();
        definition.setId(2L);
        definition.setOwnerUserId(1L);
        definition.setStatus(ArchiveAgentSupport.AGENT_STATUS_READY);

        ArchiveAgentVersion version = new ArchiveAgentVersion();
        version.setId(3L);
        version.setAgentId(2L);

        when(archiveAgentDefinitionMapper.selectById(2L)).thenReturn(definition);
        when(archiveAgentVersionMapper.selectOne(any())).thenReturn(version);
        doAnswer(invocation -> {
            ArchiveAgentRun run = invocation.getArgument(0);
            run.setId(5L);
            return 1;
        }).when(archiveAgentRunMapper).insert(any(ArchiveAgentRun.class));

        ArchiveAgentRunDTO dto = new ArchiveAgentRunDTO();
        dto.setTriggerSource("ui");
        dto.setInputPayload(Map.of("documentCode", "DOC-1"));

        ArchiveAgentRunVO result = support.runAgent(1L, 2L, dto);

        assertEquals(5L, result.getId());
        assertEquals(ArchiveAgentSupport.RUN_STATUS_PENDING, result.getStatus());
        ArgumentCaptor<ArchiveAgentRun> runCaptor = ArgumentCaptor.forClass(ArchiveAgentRun.class);
        verify(archiveAgentRunMapper).insert(runCaptor.capture());
        assertEquals(ArchiveAgentSupport.TRIGGER_SOURCE_WORKBENCH_TEST,
                runCaptor.getValue().getTriggerSource());
        verify(triggerDispatcher).dispatch(any(ArchiveAgentRun.class));
    }

    @Test
    void runAgentRejectsDisabledAgent() {
        ArchiveAgentDefinition definition = new ArchiveAgentDefinition();
        definition.setId(2L);
        definition.setOwnerUserId(1L);
        definition.setStatus(ArchiveAgentSupport.AGENT_STATUS_DISABLED);

        when(archiveAgentDefinitionMapper.selectById(2L)).thenReturn(definition);

        assertThrows(IllegalStateException.class, () -> support.runAgent(1L, 2L, new ArchiveAgentRunDTO()));
    }

    @Test
    void getRunDetailBuildsStepsAndArtifacts() {
        ArchiveAgentRun run = new ArchiveAgentRun();
        run.setId(11L);
        run.setRunNo("ARUN001");
        run.setAgentId(2L);
        run.setAgentVersionId(3L);
        run.setOwnerUserId(1L);
        run.setStatus("SUCCESS");
        run.setInputJson("{\"k\":\"v\"}");
        run.setOutputJson("{\"done\":true}");
        run.setStartedAt(LocalDateTime.of(2026, 4, 11, 20, 0));
        run.setFinishedAt(LocalDateTime.of(2026, 4, 11, 20, 1));

        ArchiveAgentDefinition definition = new ArchiveAgentDefinition();
        definition.setAgentName("Duck");

        ArchiveAgentVersion version = new ArchiveAgentVersion();
        version.setVersionNo(4);

        ArchiveAgentRunStep step = new ArchiveAgentRunStep();
        step.setRunId(11L);
        step.setStepNo(1);
        step.setNodeKey("start-1");
        step.setNodeType("start");
        step.setStatus("SUCCESS");
        step.setInputJson("{\"x\":1}");
        step.setOutputJson("{\"y\":2}");

        ArchiveAgentRunArtifact artifact = new ArchiveAgentRunArtifact();
        artifact.setRunId(11L);
        artifact.setArtifactKey("final-output");
        artifact.setArtifactType("JSON");
        artifact.setContentJson("{\"z\":3}");

        when(archiveAgentRunMapper.selectById(11L)).thenReturn(run);
        when(archiveAgentDefinitionMapper.selectById(2L)).thenReturn(definition);
        when(archiveAgentVersionMapper.selectById(3L)).thenReturn(version);
        when(archiveAgentRunStepMapper.selectList(any())).thenReturn(List.of(step));
        when(archiveAgentRunArtifactMapper.selectList(any())).thenReturn(List.of(artifact));

        ArchiveAgentRunDetailVO detail = support.getRunDetail(1L, 11L);

        assertEquals("Duck", detail.getAgentName());
        assertEquals(4, detail.getAgentVersionNo());
        assertEquals(1, detail.getSteps().size());
        assertEquals(1, detail.getArtifacts().size());
        assertEquals("final-output", detail.getArtifacts().get(0).get("artifactKey"));
    }

    @Test
    void controlledQueryShowsStepProgressWithoutStoredBusinessData() throws Exception {
        ArchiveAgentRun run = new ArchiveAgentRun();
        run.setId(12L);
        run.setAgentId(2L);
        run.setAgentVersionId(3L);
        run.setOwnerUserId(1L);
        run.setTriggerType(ArchiveAgentSupport.TRIGGER_TYPE_MANUAL);
        run.setTriggerSource("FINWORK");
        run.setStatus("SUCCESS");
        run.setSummary("secret summary");
        run.setInputJson("{\"secret\":\"run input\"}");
        run.setOutputJson("{\"secret\":\"run output\"}");

        ArchiveAgentVersion version = new ArchiveAgentVersion();
        version.setConfigJson("{\"toolCode\":\"expense.query_my_expenses\"}");

        ArchiveAgentRunStep step = new ArchiveAgentRunStep();
        step.setStepNo(1);
        step.setNodeKey("secret node key");
        step.setNodeType("tool");
        step.setNodeLabel("secret node label");
        step.setStatus("SUCCESS");
        step.setDurationMs(23L);
        step.setErrorMessage("secret error");
        step.setInputJson("{\"secret\":\"step input\"}");
        step.setOutputJson("{\"secret\":\"step output\"}");

        when(archiveAgentRunMapper.selectById(12L)).thenReturn(run);
        when(archiveAgentVersionMapper.selectById(3L)).thenReturn(version);
        when(archiveAgentRunStepMapper.selectList(any())).thenReturn(List.of(step));

        ArchiveAgentRunDetailVO detail = support.getRunDetail(1L, 12L);

        assertEquals("受控查询运行成功", detail.getSummary());
        assertEquals(Map.of("protected", true), detail.getOutputPayload());
        assertTrue(detail.getInputPayload().isEmpty());
        assertTrue(detail.getArtifacts().isEmpty());
        assertEquals(1, detail.getSteps().size());
        assertEquals("工具调用", detail.getSteps().get(0).getNodeLabel());
        assertEquals("step_1", detail.getSteps().get(0).getNodeKey());
        assertEquals("SUCCESS", detail.getSteps().get(0).getStatus());
        assertEquals(23L, detail.getSteps().get(0).getDurationMs());
        assertTrue(detail.getSteps().get(0).getInputPayload().isEmpty());
        assertTrue(detail.getSteps().get(0).getOutputPayload().isEmpty());
        assertNull(detail.getSteps().get(0).getErrorMessage());
        assertFalse(new ObjectMapper().writeValueAsString(detail).contains("secret"));
    }

    @Test
    void workbenchTestShowsStoredControlledQueryStepDetailsToOwner() {
        ArchiveAgentRun run = new ArchiveAgentRun();
        run.setId(13L);
        run.setAgentId(2L);
        run.setAgentVersionId(3L);
        run.setOwnerUserId(1L);
        run.setTriggerType(ArchiveAgentSupport.TRIGGER_TYPE_MANUAL);
        run.setTriggerSource("workbench.manual");
        run.setStatus("SUCCESS");
        run.setInputJson("{\"query\":\"test question\"}");
        run.setOutputJson("{\"recordCount\":2}");

        ArchiveAgentVersion version = new ArchiveAgentVersion();
        version.setConfigJson("{\"toolCode\":\"expense.query_my_expenses\"}");
        ArchiveAgentRunStep step = new ArchiveAgentRunStep();
        step.setRunId(13L);
        step.setStepNo(1);
        step.setNodeKey("tool_1");
        step.setNodeType("tool");
        step.setNodeLabel("查询单据");
        step.setStatus("SUCCESS");
        step.setInputJson("{\"toolCode\":\"expense.query_my_expenses\"}");
        step.setOutputJson("{\"recordCount\":2}");

        when(archiveAgentRunMapper.selectById(13L)).thenReturn(run);
        when(archiveAgentVersionMapper.selectById(3L)).thenReturn(version);
        when(archiveAgentRunStepMapper.selectList(any())).thenReturn(List.of(step));
        when(archiveAgentRunArtifactMapper.selectList(any())).thenReturn(List.of());

        ArchiveAgentRunDetailVO detail = support.getRunDetail(1L, 13L);

        assertEquals("test question", detail.getInputPayload().get("query"));
        assertEquals(2, detail.getOutputPayload().get("recordCount"));
        assertEquals("查询单据", detail.getSteps().get(0).getNodeLabel());
        assertEquals("expense.query_my_expenses", detail.getSteps().get(0).getInputPayload().get("toolCode"));
        assertEquals(2, detail.getSteps().get(0).getOutputPayload().get("recordCount"));
        run.setTriggerSource(ArchiveAgentSupport.TRIGGER_SOURCE_WORKBENCH_TEST);
        assertEquals("查询单据", support.getRunDetail(1L, 13L).getSteps().get(0).getNodeLabel());
        assertThrows(SecurityException.class, () -> support.getRunDetail(2L, 13L));
    }

    @Test
    void scheduledControlledRunCannotUseWorkbenchSourceToRevealDetails() {
        ArchiveAgentRun run = new ArchiveAgentRun();
        run.setId(14L);
        run.setAgentId(2L);
        run.setAgentVersionId(3L);
        run.setOwnerUserId(1L);
        run.setTriggerType(ArchiveAgentSupport.TRIGGER_TYPE_SCHEDULE);
        run.setTriggerSource("workbench.manual");
        run.setOutputJson("{\"secret\":\"scheduled result\"}");
        ArchiveAgentVersion version = new ArchiveAgentVersion();
        version.setConfigJson("{\"toolCode\":\"expense.query_my_expenses\"}");

        when(archiveAgentRunMapper.selectById(14L)).thenReturn(run);
        when(archiveAgentVersionMapper.selectById(3L)).thenReturn(version);
        when(archiveAgentRunStepMapper.selectList(any())).thenReturn(List.of());

        ArchiveAgentRunDetailVO detail = support.getRunDetail(1L, 14L);

        assertEquals(Map.of("protected", true), detail.getOutputPayload());
    }

    @Test
    void controlledQueryRunListHidesLegacySummariesAndErrors() {
        ArchiveAgentDefinition definition = new ArchiveAgentDefinition();
        definition.setId(2L);
        definition.setOwnerUserId(1L);
        ArchiveAgentRun run = new ArchiveAgentRun();
        run.setId(12L);
        run.setAgentVersionId(3L);
        run.setStatus("FAILED");
        run.setSummary("secret summary");
        run.setErrorMessage("secret error");
        ArchiveAgentVersion version = new ArchiveAgentVersion();
        version.setId(3L);
        version.setConfigJson("{\"toolCode\":\"expense.query_my_expenses\"}");

        when(archiveAgentDefinitionMapper.selectById(2L)).thenReturn(definition);
        when(archiveAgentRunMapper.selectList(any())).thenReturn(List.of(run));
        when(archiveAgentVersionMapper.selectBatchIds(any())).thenReturn(List.of(version));

        List<ArchiveAgentRunVO> runs = support.listRuns(1L, 2L);

        assertEquals("受控查询运行失败", runs.get(0).getSummary());
        assertEquals("受控查询失败，敏感错误详情已隐藏", runs.get(0).getErrorMessage());
    }
}
