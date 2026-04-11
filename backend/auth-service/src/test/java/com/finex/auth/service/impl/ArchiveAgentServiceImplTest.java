package com.finex.auth.service.impl;

import com.finex.auth.dto.ArchiveAgentMetaVO;
import com.finex.auth.dto.ArchiveAgentRunVO;
import com.finex.auth.dto.ArchiveAgentSummaryVO;
import com.finex.auth.service.impl.archiveagent.ArchiveAgentDefinitionDomainSupport;
import com.finex.auth.service.impl.archiveagent.ArchiveAgentMetaSupport;
import com.finex.auth.service.impl.archiveagent.ArchiveAgentRunDomainSupport;
import com.finex.auth.service.impl.archiveagent.ArchiveAgentScheduleDomainSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveAgentServiceImplTest {

    @Mock
    private ArchiveAgentMetaSupport archiveAgentMetaSupport;

    @Mock
    private ArchiveAgentDefinitionDomainSupport archiveAgentDefinitionDomainSupport;

    @Mock
    private ArchiveAgentRunDomainSupport archiveAgentRunDomainSupport;

    @Mock
    private ArchiveAgentScheduleDomainSupport archiveAgentScheduleDomainSupport;

    @Test
    void getMetaDelegatesToMetaSupport() {
        ArchiveAgentServiceImpl service = new ArchiveAgentServiceImpl(
                archiveAgentMetaSupport,
                archiveAgentDefinitionDomainSupport,
                archiveAgentRunDomainSupport,
                archiveAgentScheduleDomainSupport
        );
        ArchiveAgentMetaVO expected = new ArchiveAgentMetaVO();
        when(archiveAgentMetaSupport.getMeta()).thenReturn(expected);

        ArchiveAgentMetaVO result = service.getMeta();

        assertSame(expected, result);
        verify(archiveAgentMetaSupport).getMeta();
    }

    @Test
    void listAgentsDelegatesToDefinitionSupport() {
        ArchiveAgentServiceImpl service = new ArchiveAgentServiceImpl(
                archiveAgentMetaSupport,
                archiveAgentDefinitionDomainSupport,
                archiveAgentRunDomainSupport,
                archiveAgentScheduleDomainSupport
        );
        ArchiveAgentSummaryVO summary = new ArchiveAgentSummaryVO();
        when(archiveAgentDefinitionDomainSupport.listAgents(1L, "duck", "READY")).thenReturn(List.of(summary));

        List<ArchiveAgentSummaryVO> result = service.listAgents(1L, "duck", "READY");

        assertSame(summary, result.get(0));
        verify(archiveAgentDefinitionDomainSupport).listAgents(1L, "duck", "READY");
    }

    @Test
    void runAgentDelegatesToRunSupport() {
        ArchiveAgentServiceImpl service = new ArchiveAgentServiceImpl(
                archiveAgentMetaSupport,
                archiveAgentDefinitionDomainSupport,
                archiveAgentRunDomainSupport,
                archiveAgentScheduleDomainSupport
        );
        ArchiveAgentRunVO expected = new ArchiveAgentRunVO();
        when(archiveAgentRunDomainSupport.runAgent(1L, 2L, null)).thenReturn(expected);

        ArchiveAgentRunVO result = service.runAgent(1L, 2L, null);

        assertSame(expected, result);
        verify(archiveAgentRunDomainSupport).runAgent(1L, 2L, null);
    }
}
