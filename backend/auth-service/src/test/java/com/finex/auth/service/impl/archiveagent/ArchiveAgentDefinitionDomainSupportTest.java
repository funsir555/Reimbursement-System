package com.finex.auth.service.impl.archiveagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ArchiveAgentDetailVO;
import com.finex.auth.dto.ArchiveAgentSaveDTO;
import com.finex.auth.dto.ArchiveAgentSummaryVO;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentSchedule;
import com.finex.auth.entity.ArchiveAgentTrigger;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveAgentDefinitionDomainSupportTest {

    @Mock private ArchiveAgentDefinitionMapper archiveAgentDefinitionMapper;
    @Mock private ArchiveAgentVersionMapper archiveAgentVersionMapper;
    @Mock private ArchiveAgentTriggerMapper archiveAgentTriggerMapper;
    @Mock private ArchiveAgentToolBindingMapper archiveAgentToolBindingMapper;
    @Mock private ArchiveAgentRunMapper archiveAgentRunMapper;
    @Mock private ArchiveAgentRunStepMapper archiveAgentRunStepMapper;
    @Mock private ArchiveAgentRunArtifactMapper archiveAgentRunArtifactMapper;
    @Mock private ArchiveAgentScheduleMapper archiveAgentScheduleMapper;
    @Mock private TriggerDispatcher triggerDispatcher;

    private ArchiveAgentDefinitionDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new ArchiveAgentDefinitionDomainSupport(AbstractArchiveAgentSupport.dependencies(
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
    void listAgentsBuildsSummaryFromDefinitionAndTriggerState() {
        ArchiveAgentDefinition definition = new ArchiveAgentDefinition();
        definition.setId(10L);
        definition.setOwnerUserId(1L);
        definition.setAgentCode("AG001");
        definition.setAgentName("Duck Agent");
        definition.setStatus(ArchiveAgentSupport.AGENT_STATUS_READY);
        definition.setLatestVersionNo(3);
        definition.setPublishedVersionId(99L);
        definition.setTagsJson("[\"finance\"]");
        definition.setLastRunAt(LocalDateTime.of(2026, 4, 11, 21, 0));

        ArchiveAgentTrigger enabled = new ArchiveAgentTrigger();
        enabled.setAgentId(10L);
        enabled.setEnabled(1);
        ArchiveAgentTrigger disabled = new ArchiveAgentTrigger();
        disabled.setAgentId(10L);
        disabled.setEnabled(0);

        ArchiveAgentVersion version = new ArchiveAgentVersion();
        version.setId(99L);
        version.setVersionNo(2);

        when(archiveAgentDefinitionMapper.selectList(any())).thenReturn(List.of(definition));
        when(archiveAgentTriggerMapper.selectList(any())).thenReturn(List.of(enabled, disabled));
        when(archiveAgentVersionMapper.selectBatchIds(any())).thenReturn(List.of(version));

        List<ArchiveAgentSummaryVO> result = support.listAgents(1L, "Duck", "ready");

        assertEquals(1, result.size());
        assertEquals("AG001", result.get(0).getAgentCode());
        assertEquals(1, result.get(0).getEnabledTriggerCount());
        assertEquals(2, result.get(0).getPublishedVersionNo());
        assertEquals("READY", result.get(0).getRuntimeStatus());
    }

    @Test
    void createAgentCreatesVersionBindingsAndSchedule() {
        ArchiveAgentSaveDTO dto = new ArchiveAgentSaveDTO();
        dto.setAgentName("Expense Duck");
        dto.setTags(List.of("finance"));
        dto.setPromptConfig(Map.of("systemPrompt", "test"));
        dto.setModelConfig(Map.of("provider", "MOCK"));
        dto.setTools(List.of(Map.of("toolCode", "notify.send_message", "enabled", true)));
        dto.setWorkflow(new LinkedHashMap<>(Map.of(
                "nodes", List.of(
                        Map.of("nodeKey", "start-1", "nodeType", "start"),
                        Map.of("nodeKey", "end-1", "nodeType", "end")
                ),
                "edges", List.of(Map.of("source", "start-1", "target", "end-1"))
        )));
        dto.setTriggers(List.of(Map.of(
                "triggerType", "SCHEDULE",
                "enabled", true,
                "scheduleMode", "INTERVAL",
                "intervalMinutes", 30
        )));

        AtomicReference<ArchiveAgentDefinition> definitionRef = new AtomicReference<>();
        AtomicReference<ArchiveAgentVersion> versionRef = new AtomicReference<>();

        doAnswer(invocation -> {
            ArchiveAgentDefinition definition = invocation.getArgument(0);
            definition.setId(10L);
            definitionRef.set(definition);
            return 1;
        }).when(archiveAgentDefinitionMapper).insert(any(ArchiveAgentDefinition.class));
        doAnswer(invocation -> {
            ArchiveAgentVersion version = invocation.getArgument(0);
            version.setId(20L);
            versionRef.set(version);
            return 1;
        }).when(archiveAgentVersionMapper).insert(any(ArchiveAgentVersion.class));
        doAnswer(invocation -> {
            ArchiveAgentTrigger trigger = invocation.getArgument(0);
            trigger.setId(30L);
            return 1;
        }).when(archiveAgentTriggerMapper).insert(any(ArchiveAgentTrigger.class));
        when(archiveAgentDefinitionMapper.selectById(10L)).thenAnswer(invocation -> definitionRef.get());
        when(archiveAgentVersionMapper.selectOne(any())).thenAnswer(invocation -> versionRef.get());
        when(archiveAgentVersionMapper.selectList(any())).thenAnswer(invocation -> List.of(versionRef.get()));

        ArchiveAgentDetailVO detail = support.createAgent(1L, "tester", dto);

        assertEquals(10L, detail.getId());
        assertEquals(1, detail.getLatestVersionNo());
        assertEquals(1, detail.getVersions().size());
        assertNotNull(detail.getWorkflow());
        verify(archiveAgentToolBindingMapper).insert(any());
        verify(archiveAgentScheduleMapper).insert(any(ArchiveAgentSchedule.class));
        verify(archiveAgentDefinitionMapper).updateById(any(ArchiveAgentDefinition.class));
    }

    @Test
    void createAgentRejectsWorkflowWithoutStartNode() {
        ArchiveAgentSaveDTO dto = new ArchiveAgentSaveDTO();
        dto.setAgentName("Broken");
        dto.setWorkflow(Map.of(
                "nodes", List.of(Map.of("nodeKey", "end-1", "nodeType", "end")),
                "edges", List.of()
        ));

        assertThrows(IllegalArgumentException.class, () -> support.createAgent(1L, "tester", dto));
    }
}
