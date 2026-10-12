package com.finex.auth.service.impl.archiveagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finex.auth.dto.ArchiveAgentToolsSaveDTO;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentVersion;
import com.finex.auth.entity.ArchiveAgentToolBinding;
import com.finex.auth.mapper.ArchiveAgentDefinitionMapper;
import com.finex.auth.mapper.ArchiveAgentRunArtifactMapper;
import com.finex.auth.mapper.ArchiveAgentRunMapper;
import com.finex.auth.mapper.ArchiveAgentRunStepMapper;
import com.finex.auth.mapper.ArchiveAgentScheduleMapper;
import com.finex.auth.mapper.ArchiveAgentToolBindingMapper;
import com.finex.auth.mapper.ArchiveAgentTriggerMapper;
import com.finex.auth.mapper.ArchiveAgentVersionMapper;
import com.finex.auth.support.archiveagent.TriggerDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveAgentToolBindingUpdateTest {
    @Mock ArchiveAgentDefinitionMapper definitions;
    @Mock ArchiveAgentVersionMapper versions;
    @Mock ArchiveAgentTriggerMapper triggers;
    @Mock ArchiveAgentToolBindingMapper bindings;
    @Mock ArchiveAgentRunMapper runs;
    @Mock ArchiveAgentRunStepMapper steps;
    @Mock ArchiveAgentRunArtifactMapper artifacts;
    @Mock ArchiveAgentScheduleMapper schedules;
    @Mock TriggerDispatcher dispatcher;
    private final ObjectMapper json = new ObjectMapper();
    private ArchiveAgentDefinitionDomainSupport support;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, ArchiveAgentDefinition.class);
        TableInfoHelper.initTableInfo(assistant, ArchiveAgentVersion.class);
        TableInfoHelper.initTableInfo(assistant, ArchiveAgentToolBinding.class);
        support = new ArchiveAgentDefinitionDomainSupport(AbstractArchiveAgentSupport.dependencies(
                definitions, versions, triggers, bindings, runs, steps, artifacts, schedules, json, dispatcher));
        ArchiveAgentDefinition definition = new ArchiveAgentDefinition();
        definition.setId(9L);
        definition.setOwnerUserId(7L);
        definition.setLatestVersionNo(3);
        definition.setAgentName("测试 Agent");
        when(definitions.selectById(9L)).thenReturn(definition);
        ArchiveAgentVersion latest = new ArchiveAgentVersion();
        latest.setAgentId(9L);
        latest.setVersionNo(3);
        latest.setConfigJson("{\"tools\":[{\"toolCode\":\"notify.send_message\",\"title\":\"原通知\"},{\"toolCode\":\"expense.query_my_expenses\",\"custom\":\"保留\"}],\"workflow\":{\"nodes\":[]}}");
        lenient().when(versions.selectOne(any())).thenReturn(latest);
    }

    @Test
    void savePreservesLegacyBindingAndWorkflow() throws Exception {
        when(definitions.update(any(), any())).thenReturn(1);
        when(versions.selectList(any())).thenReturn(List.of());
        ArchiveAgentToolsSaveDTO dto = new ArchiveAgentToolsSaveDTO();
        dto.setExpectedVersionNo(3);
        dto.setToolCodes(List.of("expense.query_my_expenses", "finance.query_unclosed_companies"));

        support.updateTools(7L, 9L, "tester", dto);

        ArgumentCaptor<ArchiveAgentVersion> captured = ArgumentCaptor.forClass(ArchiveAgentVersion.class);
        verify(versions).insert(captured.capture());
        var config = json.readTree(captured.getValue().getConfigJson());
        assertEquals(4, captured.getValue().getVersionNo());
        assertEquals("原通知", config.path("tools").get(0).path("title").asText());
        assertEquals("保留", config.path("tools").get(1).path("custom").asText());
        assertEquals("finance.query_unclosed_companies", config.path("tools").get(2).path("toolCode").asText());
        assertEquals(0, config.path("workflow").path("nodes").size());
    }

    @Test
    void versionConflictDoesNotCreateNewVersion() {
        ArchiveAgentToolsSaveDTO dto = new ArchiveAgentToolsSaveDTO();
        dto.setExpectedVersionNo(2);
        dto.setToolCodes(List.of());
        assertThrows(IllegalStateException.class, () -> support.updateTools(7L, 9L, "tester", dto));
        verify(versions, never()).insert(any());
    }
}
