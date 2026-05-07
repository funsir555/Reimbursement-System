package com.finex.auth.service.impl;

import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseBankLinkSummaryVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentPickerVO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.dto.ExpenseManualApproverSelectionDTO;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import com.finex.auth.service.impl.expense.ExpenseRelationWriteOffService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentServiceImplTest {

    @Mock
    private ExpenseDocumentSubmissionService expenseDocumentSubmissionService;
    @Mock
    private ExpenseDocumentQueryService expenseDocumentQueryService;
    @Mock
    private ExpenseApprovalWorkflowService expenseApprovalWorkflowService;
    @Mock
    private ExpensePaymentWorkflowService expensePaymentWorkflowService;
    @Mock
    private ExpenseMaintenanceService expenseMaintenanceService;
    @Mock
    private ExpenseRelationWriteOffService expenseRelationWriteOffService;

    @Test
    void listAvailableTemplatesDelegatesToSubmissionService() {
        ExpenseCreateTemplateSummaryVO summary = new ExpenseCreateTemplateSummaryVO();
        List<ExpenseCreateTemplateSummaryVO> expected = List.of(summary);
        ExpenseDocumentServiceImpl service = newService();
        when(expenseDocumentSubmissionService.listAvailableTemplates()).thenReturn(expected);

        List<ExpenseCreateTemplateSummaryVO> actual = service.listAvailableTemplates();

        assertSame(expected, actual);
        verify(expenseDocumentSubmissionService).listAvailableTemplates();
    }

    @Test
    void getDocumentPickerDelegatesToRelationService() {
        ExpenseDocumentPickerVO expected = new ExpenseDocumentPickerVO();
        ExpenseDocumentServiceImpl service = newService();
        when(expenseRelationWriteOffService.getDocumentPicker(1L, "RELATED", List.of("report"), "kw", 1, 10, "DOC-1", false))
                .thenReturn(expected);

        ExpenseDocumentPickerVO actual = service.getDocumentPicker(1L, "RELATED", List.of("report"), "kw", 1, 10, "DOC-1", false);

        assertSame(expected, actual);
        verify(expenseRelationWriteOffService).getDocumentPicker(1L, "RELATED", List.of("report"), "kw", 1, 10, "DOC-1", false);
    }

    @Test
    void approveTaskDelegatesToApprovalWorkflowService() {
        ExpenseApprovalActionDTO dto = new ExpenseApprovalActionDTO();
        ExpenseDocumentServiceImpl service = newService();
        when(expenseApprovalWorkflowService.approveTask(1L, "tester", 99L, dto)).thenReturn(null);

        service.approveTask(1L, "tester", 99L, dto);

        verify(expenseApprovalWorkflowService).approveTask(1L, "tester", 99L, dto);
    }

    @Test
    void listPaymentOrdersDelegatesToPaymentWorkflowService() {
        ExpensePaymentOrderVO order = new ExpensePaymentOrderVO();
        List<ExpensePaymentOrderVO> expected = List.of(order);
        ExpenseDocumentServiceImpl service = newService();
        when(expensePaymentWorkflowService.listPaymentOrders(1L, "PENDING")).thenReturn(expected);

        List<ExpensePaymentOrderVO> actual = service.listPaymentOrders(1L, "PENDING");

        assertSame(expected, actual);
        verify(expensePaymentWorkflowService).listPaymentOrders(1L, "PENDING");
    }

    @Test
    void rejectPaymentTasksDelegatesToPaymentWorkflowService() {
        ExpenseApprovalActionDTO dto = new ExpenseApprovalActionDTO();
        ExpenseDocumentServiceImpl service = newService();
        when(expensePaymentWorkflowService.rejectPaymentTasks(1L, "tester", List.of(20L, 21L), dto)).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertTrue(service.rejectPaymentTasks(1L, "tester", List.of(20L, 21L), dto));

        verify(expensePaymentWorkflowService).rejectPaymentTasks(1L, "tester", List.of(20L, 21L), dto);
    }

    @Test
    void submitManualApproverSelectionDelegatesToQueryService() {
        ExpenseManualApproverSelectionDTO dto = new ExpenseManualApproverSelectionDTO();
        dto.setNodeKey("approval-manual");
        dto.setUserIds(List.of(8L));
        ExpenseDocumentDetailVO expected = new ExpenseDocumentDetailVO();
        ExpenseDocumentServiceImpl service = newService();
        when(expenseDocumentQueryService.submitManualApproverSelection(1L, "tester", "DOC-001", dto)).thenReturn(expected);

        ExpenseDocumentDetailVO actual = service.submitManualApproverSelection(1L, "tester", "DOC-001", dto);

        assertSame(expected, actual);
        verify(expenseDocumentQueryService).submitManualApproverSelection(1L, "tester", "DOC-001", dto);
    }

    @Test
    void deleteDraftDocumentDelegatesToQueryService() {
        ExpenseDocumentServiceImpl service = newService();
        when(expenseDocumentQueryService.deleteDraftDocument(1L, "DOC-001", false)).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertTrue(service.deleteDraftDocument(1L, "DOC-001", false));

        verify(expenseDocumentQueryService).deleteDraftDocument(1L, "DOC-001", false);
    }

    @Test
    void saveDraftDocumentDelegatesToSubmissionService() {
        ExpenseDocumentServiceImpl service = newService();
        ExpenseDocumentUpdateDTO dto = new ExpenseDocumentUpdateDTO();
        ExpenseDocumentEditContextVO expected = new ExpenseDocumentEditContextVO();
        when(expenseDocumentSubmissionService.saveDraftDocument(1L, "DOC-001", dto)).thenReturn(expected);

        ExpenseDocumentEditContextVO actual = service.saveDraftDocument(1L, "DOC-001", dto);

        assertSame(expected, actual);
        verify(expenseDocumentSubmissionService).saveDraftDocument(1L, "DOC-001", dto);
    }

    @Test
    void repairMisapprovedDocumentsDelegatesToMaintenanceService() {
        List<String> expected = List.of("DOC-001");
        ExpenseDocumentServiceImpl service = newService();
        when(expenseMaintenanceService.repairMisapprovedDocumentsByRootContainerBug()).thenReturn(expected);

        List<String> actual = service.repairMisapprovedDocumentsByRootContainerBug();

        assertSame(expected, actual);
        verify(expenseMaintenanceService).repairMisapprovedDocumentsByRootContainerBug();
    }

    private ExpenseDocumentServiceImpl newService() {
        return new ExpenseDocumentServiceImpl(
                expenseDocumentSubmissionService,
                expenseDocumentQueryService,
                expenseApprovalWorkflowService,
                expensePaymentWorkflowService,
                expenseMaintenanceService,
                expenseRelationWriteOffService
        );
    }
}
