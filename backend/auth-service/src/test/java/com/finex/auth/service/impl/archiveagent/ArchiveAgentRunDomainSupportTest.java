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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
