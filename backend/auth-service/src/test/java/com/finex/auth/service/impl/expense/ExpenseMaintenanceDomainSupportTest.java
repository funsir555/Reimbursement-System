package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseMaintenanceDomainSupportTest {

    @Mock
    private ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    @Mock
    private ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;

    @Test
    void repairUsesRuntimePrimitives() {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");
        instance.setFlowSnapshotJson("{}");
        List<String> expected = List.of("DOC-001");
        ExpenseMaintenanceDomainSupport support = new ExpenseMaintenanceDomainSupport(
                processDocumentInstanceMapper,
                expenseWorkflowRuntimeSupport
        );
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(instance));
        when(expenseWorkflowRuntimeSupport.inspectRawFlowSnapshot("{}"))
                .thenReturn(new RawFlowSnapshotSignature(true, true, false));
        when(expenseWorkflowRuntimeSupport.isMisapprovedByBlankRootBug("DOC-001")).thenReturn(true);

        List<String> actual = support.repairMisapprovedDocumentsByRootContainerBug();

        assertEquals(expected, actual);
        verify(expenseWorkflowRuntimeSupport).rebuildMisapprovedRuntime(instance);
    }
}
