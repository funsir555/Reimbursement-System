package com.finex.auth.service.impl;

import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseBankLinkSummaryVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseDocumentPickerVO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
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
        ExpenseDocumentServiceImpl service = new ExpenseDocumentServiceImpl(
                expenseDocumentSubmissionService,
                expenseDocumentQueryService,
                expenseApprovalWorkflowService,
                expensePaymentWorkflowService,
                expenseMaintenanceService,
                expenseRelationWriteOffService
        );
        when(expenseDocumentSubmissionService.listAvailableTemplates()).thenReturn(expected);

        List<ExpenseCreateTemplateSummaryVO> actual = service.listAvailableTemplates();

        assertSame(expected, actual);
        verify(expenseDocumentSubmissionService).listAvailableTemplates();
    }

    @Test
    void getDocumentPickerDelegatesToRelationService() {
        ExpenseDocumentPickerVO expected = new ExpenseDocumentPickerVO();
        ExpenseDocumentServiceImpl service = new ExpenseDocumentServiceImpl(
                expenseDocumentSubmissionService,
                expenseDocumentQueryService,
                expenseApprovalWorkflowService,
                expensePaymentWorkflowService,
                expenseMaintenanceService,
                expenseRelationWriteOffService
        );
        when(expenseRelationWriteOffService.getDocumentPicker(1L, "RELATED", List.of("report"), "kw", 1, 10, "DOC-1", false))
                .thenReturn(expected);

        ExpenseDocumentPickerVO actual = service.getDocumentPicker(1L, "RELATED", List.of("report"), "kw", 1, 10, "DOC-1", false);

        assertSame(expected, actual);
        verify(expenseRelationWriteOffService).getDocumentPicker(1L, "RELATED", List.of("report"), "kw", 1, 10, "DOC-1", false);
    }

    @Test
    void approveTaskDelegatesToApprovalWorkflowService() {
        ExpenseApprovalActionDTO dto = new ExpenseApprovalActionDTO();
        ExpenseDocumentServiceImpl service = new ExpenseDocumentServiceImpl(
                expenseDocumentSubmissionService,
                expenseDocumentQueryService,
                expenseApprovalWorkflowService,
                expensePaymentWorkflowService,
                expenseMaintenanceService,
                expenseRelationWriteOffService
        );
        when(expenseApprovalWorkflowService.approveTask(1L, "tester", 99L, dto)).thenReturn(null);

        service.approveTask(1L, "tester", 99L, dto);

        verify(expenseApprovalWorkflowService).approveTask(1L, "tester", 99L, dto);
    }

    @Test
    void listPaymentOrdersDelegatesToPaymentWorkflowService() {
        ExpensePaymentOrderVO order = new ExpensePaymentOrderVO();
        List<ExpensePaymentOrderVO> expected = List.of(order);
        ExpenseDocumentServiceImpl service = new ExpenseDocumentServiceImpl(
                expenseDocumentSubmissionService,
                expenseDocumentQueryService,
                expenseApprovalWorkflowService,
                expensePaymentWorkflowService,
                expenseMaintenanceService,
                expenseRelationWriteOffService
        );
        when(expensePaymentWorkflowService.listPaymentOrders(1L, "PENDING")).thenReturn(expected);

        List<ExpensePaymentOrderVO> actual = service.listPaymentOrders(1L, "PENDING");

        assertSame(expected, actual);
        verify(expensePaymentWorkflowService).listPaymentOrders(1L, "PENDING");
    }

    @Test
    void rejectPaymentTasksDelegatesToPaymentWorkflowService() {
        ExpenseApprovalActionDTO dto = new ExpenseApprovalActionDTO();
        ExpenseDocumentServiceImpl service = new ExpenseDocumentServiceImpl(
                expenseDocumentSubmissionService,
                expenseDocumentQueryService,
                expenseApprovalWorkflowService,
                expensePaymentWorkflowService,
                expenseMaintenanceService,
                expenseRelationWriteOffService
        );
        when(expensePaymentWorkflowService.rejectPaymentTasks(1L, "tester", List.of(20L, 21L), dto)).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertTrue(service.rejectPaymentTasks(1L, "tester", List.of(20L, 21L), dto));

        verify(expensePaymentWorkflowService).rejectPaymentTasks(1L, "tester", List.of(20L, 21L), dto);
    }

    @Test
    void repairMisapprovedDocumentsDelegatesToMaintenanceService() {
        List<String> expected = List.of("DOC-001");
        ExpenseDocumentServiceImpl service = new ExpenseDocumentServiceImpl(
                expenseDocumentSubmissionService,
                expenseDocumentQueryService,
                expenseApprovalWorkflowService,
                expensePaymentWorkflowService,
                expenseMaintenanceService,
                expenseRelationWriteOffService
        );
        when(expenseMaintenanceService.repairMisapprovedDocumentsByRootContainerBug()).thenReturn(expected);

        List<String> actual = service.repairMisapprovedDocumentsByRootContainerBug();

        assertSame(expected, actual);
        verify(expenseMaintenanceService).repairMisapprovedDocumentsByRootContainerBug();
    }
}
