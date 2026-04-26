package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseApprovalLogVO;
import com.finex.auth.dto.ExpenseApprovalNodeStatusVO;
import com.finex.auth.dto.ExpenseApprovalTaskVO;
import com.finex.auth.dto.ExpenseApprovalTimelineItemVO;
import com.finex.auth.dto.ExpenseDetailInstanceSummaryVO;
import com.finex.auth.dto.ExpenseDocumentBankPaymentVO;
import com.finex.auth.dto.ExpenseDocumentBankReceiptVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.entity.PmBankPaymentRecord;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentDetailViewSupportTest {

    @Mock
    private AbstractExpenseDocumentSupport support;
    @Mock
    private ExpenseDocumentActionLogSupport expenseDocumentActionLogSupport;
    @Mock
    private ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;
    @Mock
    private ExpenseApprovalProjectionSupport expenseApprovalProjectionSupport;
    @Mock
    private ExpenseReadonlyPayeeAccountSnapshotEnhancer readonlyPayeeAccountSnapshotEnhancer;

    @Test
    void buildDocumentDetailDelegatesToDetailOwnersAndEnrichesManualSelectionAndBankPayment() {
        ExpenseDocumentDetailViewSupport detailViewSupport = new ExpenseDocumentDetailViewSupport(
                support,
                expenseDocumentActionLogSupport,
                expenseWorkflowRuntimeSupport,
                expenseApprovalProjectionSupport,
                readonlyPayeeAccountSnapshotEnhancer
        );
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-100");
        instance.setTemplateType("report");
        instance.setStatus("PENDING_APPROVAL");
        instance.setCurrentTaskType("MANUAL_SELECT");
        instance.setCurrentNodeKey("N1");
        instance.setCurrentNodeName("指定审批人");
        instance.setTemplateSnapshotJson("{template}");
        instance.setFormSchemaSnapshotJson("{schema}");
        instance.setFormDataJson("{form}");
        instance.setFlowSnapshotJson("{flow}");
        instance.setTotalAmount(new BigDecimal("88.50"));
        instance.setFinishedAt(LocalDateTime.of(2026, 4, 26, 12, 30));

        Map<String, Object> templateSnapshot = Map.of("template", "snapshot");
        Map<String, Object> schemaSnapshot = Map.of("blocks", List.of());
        Map<String, Object> formData = Map.of("reason", "travel");
        Map<String, Object> flowSnapshot = Map.of("nodes", List.of());
        LocalDateTime submittedAt = LocalDateTime.of(2026, 4, 26, 9, 0);
        List<ProcessFormOptionVO> companyOptions = List.of(new ProcessFormOptionVO());
        List<ProcessFormOptionVO> departmentOptions = List.of(new ProcessFormOptionVO());
        List<ExpenseDetailInstanceSummaryVO> expenseDetails = List.of(new ExpenseDetailInstanceSummaryVO());
        ProcessDocumentTask pendingTask = new ProcessDocumentTask();
        pendingTask.setId(9L);
        ExpenseApprovalTaskVO currentTask = new ExpenseApprovalTaskVO();
        currentTask.setId(9L);
        ProcessDocumentActionLog actionLog = new ProcessDocumentActionLog();
        actionLog.setId(11L);
        ExpenseApprovalLogVO actionLogVo = new ExpenseApprovalLogVO();
        actionLogVo.setId(11L);
        List<ProcessDocumentTask> allTasks = List.of(pendingTask);
        FlowRuntimeSnapshot runtimeSnapshot = new FlowRuntimeSnapshot(List.of(), List.of());
        Map<String, Object> runtimeContext = Map.of("companyId", "C1");
        List<ExpenseApprovalNodeStatusVO> approvalNodeStatuses = List.of(new ExpenseApprovalNodeStatusVO());
        List<ExpenseApprovalTimelineItemVO> approvalTimeline = List.of(new ExpenseApprovalTimelineItemVO());
        List<ProcessFormOptionVO> manualApproverOptions = List.of(new ProcessFormOptionVO());
        PmBankPaymentRecord bankPaymentRecord = new PmBankPaymentRecord();
        bankPaymentRecord.setCompanyBankAccountId(7L);
        ExpenseDocumentBankPaymentVO bankPayment = new ExpenseDocumentBankPaymentVO();
        List<ExpenseDocumentBankReceiptVO> bankReceipts = List.of(new ExpenseDocumentBankReceiptVO());

        when(support.trimToNull("report")).thenReturn("report");
        when(support.trimToNull("MANUAL_SELECT")).thenReturn("MANUAL_SELECT");
        when(support.trimToNull("N1")).thenReturn("N1");
        when(support.resolveStatusLabel("PENDING_APPROVAL")).thenReturn("审批中");
        when(support.resolveDisplaySubmittedAt(instance)).thenReturn(submittedAt);
        when(support.readMap("{template}")).thenReturn(templateSnapshot);
        when(support.readMap("{schema}")).thenReturn(schemaSnapshot);
        when(support.readMap("{flow}")).thenReturn(flowSnapshot);
        when(support.readFormData("{form}")).thenReturn(formData);
        when(support.loadCompanyOptionsForDetail(schemaSnapshot, formData)).thenReturn(companyOptions);
        when(support.loadDepartmentOptionsForDetail(schemaSnapshot, formData)).thenReturn(departmentOptions);
        when(support.safeLoadExpenseDetailSummaries("DOC-100")).thenReturn(expenseDetails);
        when(support.loadPendingTasks("DOC-100")).thenReturn(List.of(pendingTask));
        when(support.toTaskVO(pendingTask)).thenReturn(currentTask);
        when(expenseDocumentActionLogSupport.loadActionLogs("DOC-100")).thenReturn(List.of(actionLog));
        when(support.toLogVO(actionLog)).thenReturn(actionLogVo);
        when(support.loadAllTasks("DOC-100")).thenReturn(allTasks);
        when(support.readFlowRuntimeSnapshot("{flow}")).thenReturn(runtimeSnapshot);
        when(expenseWorkflowRuntimeSupport.buildRuntimeContextForInstance(instance)).thenReturn(runtimeContext);
        when(expenseApprovalProjectionSupport.build(
                instance,
                runtimeSnapshot,
                runtimeContext,
                allTasks,
                List.of(actionLog)
        )).thenReturn(new ExpenseApprovalProjectionSupport.ApprovalProjectionResult(
                approvalNodeStatuses,
                approvalTimeline
        ));
        when(support.loadUserOptions(flowSnapshot)).thenReturn(manualApproverOptions);
        when(support.findLatestBankPaymentRecord("DOC-100")).thenReturn(bankPaymentRecord);
        when(support.loadCompanyBankAccountNameMap(java.util.Set.of(7L))).thenReturn(Map.of(7L, "招商银行-基本户"));
        when(support.toDetailBankPayment(bankPaymentRecord, "招商银行-基本户", "PENDING_APPROVAL")).thenReturn(bankPayment);
        when(support.toDetailBankReceipts(bankPaymentRecord)).thenReturn(bankReceipts);

        ExpenseDocumentDetailVO detail = detailViewSupport.buildDocumentDetail(instance);

        assertEquals("DOC-100", detail.getDocumentCode());
        assertEquals(new BigDecimal("88.50"), detail.getTotalAmount());
        assertSame(companyOptions, detail.getCompanyOptions());
        assertSame(departmentOptions, detail.getDepartmentOptions());
        assertSame(expenseDetails, detail.getExpenseDetails());
        assertSame(approvalNodeStatuses, detail.getApprovalNodeStatuses());
        assertSame(approvalTimeline, detail.getApprovalTimeline());
        assertTrue(detail.getManualApproverSelectionPending());
        assertSame(manualApproverOptions, detail.getManualApproverOptions());
        assertSame(bankPayment, detail.getBankPayment());
        assertSame(bankReceipts, detail.getBankReceipts());
        verify(readonlyPayeeAccountSnapshotEnhancer).enhanceFormData(schemaSnapshot, formData, null);
    }
}
