package com.finex.auth.service.impl.archiveagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ArchiveAgentMetaVO;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ArchiveAgentMetaSupportTest {

    @Mock private ArchiveAgentDefinitionMapper archiveAgentDefinitionMapper;
    @Mock private ArchiveAgentVersionMapper archiveAgentVersionMapper;
    @Mock private ArchiveAgentTriggerMapper archiveAgentTriggerMapper;
    @Mock private ArchiveAgentToolBindingMapper archiveAgentToolBindingMapper;
    @Mock private ArchiveAgentRunMapper archiveAgentRunMapper;
    @Mock private ArchiveAgentRunStepMapper archiveAgentRunStepMapper;
    @Mock private ArchiveAgentRunArtifactMapper archiveAgentRunArtifactMapper;
    @Mock private ArchiveAgentScheduleMapper archiveAgentScheduleMapper;
    @Mock private TriggerDispatcher triggerDispatcher;

    private ArchiveAgentMetaSupport support;

    @BeforeEach
    void setUp() {
        support = new ArchiveAgentMetaSupport(AbstractArchiveAgentSupport.dependencies(
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
    void getMetaReturnsStableOptions() {
        ArchiveAgentMetaVO meta = support.getMeta();

        assertEquals(4, meta.getModelProviders().size());
        assertEquals(6, meta.getTools().size());
        assertEquals(7, meta.getNodeTypes().size());
        assertEquals(3, meta.getTriggerTypes().size());
        assertEquals(3, meta.getIconOptions().size());
        assertEquals(3, meta.getThemeOptions().size());
        assertNotNull(meta.getDefaultSystemPrompt());
        assertFalse(meta.getDefaultSystemPrompt().isBlank());
    }
}
