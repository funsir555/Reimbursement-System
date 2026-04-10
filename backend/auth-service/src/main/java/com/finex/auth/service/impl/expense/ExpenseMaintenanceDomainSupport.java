package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseMaintenanceDomainSupport {

    private static final String DOCUMENT_STATUS_APPROVED = "APPROVED";
    private static final String DOCUMENT_STATUS_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    private static final String DOCUMENT_STATUS_PAYMENT_FINISHED = "PAYMENT_FINISHED";

    private final ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    private final ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;

    public List<String> repairMisapprovedDocumentsByRootContainerBug() {
        List<ProcessDocumentInstance> approvedDocuments = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .in(ProcessDocumentInstance::getStatus, List.of(
                                DOCUMENT_STATUS_APPROVED,
                                DOCUMENT_STATUS_PAYMENT_COMPLETED,
                                DOCUMENT_STATUS_PAYMENT_FINISHED
                        ))
                        .orderByAsc(ProcessDocumentInstance::getId)
        );
        if (approvedDocuments.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> repairedDocumentCodes = new ArrayList<>();
        for (ProcessDocumentInstance instance : approvedDocuments) {
            RawFlowSnapshotSignature signature = expenseWorkflowRuntimeSupport.inspectRawFlowSnapshot(instance.getFlowSnapshotJson());
            if (!signature.hasApprovalNode() || !signature.hasBlankRootNode() || signature.hasNullRootNode()) {
                continue;
            }
            if (!expenseWorkflowRuntimeSupport.isMisapprovedByBlankRootBug(instance.getDocumentCode())) {
                continue;
            }
            expenseWorkflowRuntimeSupport.rebuildMisapprovedRuntime(instance);
            repairedDocumentCodes.add(instance.getDocumentCode());
        }
        return repairedDocumentCodes;
    }
}
