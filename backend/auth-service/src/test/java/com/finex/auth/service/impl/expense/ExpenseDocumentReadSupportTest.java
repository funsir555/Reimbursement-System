package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentReadSupportTest {

    @Mock
    private AbstractExpenseDocumentSupport support;
    @Mock
    private ExpenseDocumentDetailViewSupport detailViewSupport;
    @Mock
    private ExpenseRelationWriteOffService expenseRelationWriteOffService;

    @Test
    void documentReadsDelegateToSharedSupport() {
        ExpenseDocumentReadSupport readSupport = new ExpenseDocumentReadSupport(support, detailViewSupport, expenseRelationWriteOffService);
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-1");
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        when(support.requireDocument("DOC-1")).thenReturn(instance);
        when(detailViewSupport.buildDocumentDetail(instance)).thenReturn(detail);
        when(expenseRelationWriteOffService.loadRelatedDocumentBindings("DOC-1")).thenReturn(List.of());
        when(expenseRelationWriteOffService.loadWriteOffDocumentBindings("DOC-1")).thenReturn(List.of());

        assertSame(instance, readSupport.requireDocument("DOC-1"));
        assertSame(detail, readSupport.buildDocumentDetail(instance));
        readSupport.requireSubmitter(instance, 1L);
        readSupport.assertCanViewDocument(instance, 1L, true);

        verify(support).requireSubmitter(instance, 1L);
        verify(support).assertCanViewDocument(instance, 1L, true);
    }

    @Test
    void detailAndFormReadsDelegateToSharedSupport() {
        ExpenseDocumentReadSupport readSupport = new ExpenseDocumentReadSupport(support, detailViewSupport, expenseRelationWriteOffService);
        ProcessDocumentExpenseDetail detail = new ProcessDocumentExpenseDetail();
        ExpenseDetailInstanceDTO runtimeDetail = new ExpenseDetailInstanceDTO();
        List<ProcessDocumentExpenseDetail> details = List.of(detail);
        Map<String, Object> formData = Map.of("reason", "trip");
        when(support.readFormData("{}")).thenReturn(formData);
        when(support.loadExpenseDetails("DOC-1")).thenReturn(details);
        when(support.requireExpenseDetail("DOC-1", "D1")).thenReturn(detail);
        when(support.toRuntimeExpenseDetailDTO(detail)).thenReturn(runtimeDetail);

        assertSame(formData, readSupport.readFormData("{}"));
        assertSame(details, readSupport.loadExpenseDetails("DOC-1"));
        assertSame(detail, readSupport.requireExpenseDetail("DOC-1", "D1"));
        assertSame(runtimeDetail, readSupport.toRuntimeExpenseDetailDTO(detail));
    }

    @Test
    void buildDocumentDetailEnrichesBusinessBindings() {
        ExpenseDocumentReadSupport readSupport = new ExpenseDocumentReadSupport(support, detailViewSupport, expenseRelationWriteOffService);
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-88");
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        var relatedBindings = List.of(new com.finex.auth.dto.ExpenseDocumentRelationBindingVO());
        var writeOffBindings = List.of(new com.finex.auth.dto.ExpenseDocumentWriteOffBindingVO());
        when(detailViewSupport.buildDocumentDetail(instance)).thenReturn(detail);
        when(expenseRelationWriteOffService.loadRelatedDocumentBindings("DOC-88")).thenReturn(relatedBindings);
        when(expenseRelationWriteOffService.loadWriteOffDocumentBindings("DOC-88")).thenReturn(writeOffBindings);

        ExpenseDocumentDetailVO result = readSupport.buildDocumentDetail(instance);

        assertSame(detail, result);
        assertSame(relatedBindings, result.getRelatedDocumentBindings());
        assertSame(writeOffBindings, result.getWriteOffDocumentBindings());
    }
}
