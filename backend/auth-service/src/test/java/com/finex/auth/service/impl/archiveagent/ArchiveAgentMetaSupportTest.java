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
        assertEquals(12, meta.getTools().size());
        assertEquals(1, meta.getTools().stream()
                .filter(tool -> "finance.query_supplier_detail_ledger".equals(tool.get("toolCode")))
                .count());
        assertEquals(1, meta.getTools().stream()
                .filter(tool -> "finance.query_unclosed_companies".equals(tool.get("toolCode")))
                .count());
        assertEquals(0, meta.getTools().stream()
                .filter(tool -> "invoice.query_my_invoices".equals(tool.get("toolCode"))
                        || "notify.send_message".equals(tool.get("toolCode"))
                        || "http.mock_request".equals(tool.get("toolCode"))
                        || "json.extract".equals(tool.get("toolCode"))
                        || "async_task.submit".equals(tool.get("toolCode")))
                .count());
        assertEquals(7, meta.getNodeTypes().size());
        assertEquals(3, meta.getTriggerTypes().size());
        assertEquals(3, meta.getIconOptions().size());
        assertEquals(3, meta.getThemeOptions().size());
        assertNotNull(meta.getDefaultSystemPrompt());
        assertFalse(meta.getDefaultSystemPrompt().isBlank());
    }
}
