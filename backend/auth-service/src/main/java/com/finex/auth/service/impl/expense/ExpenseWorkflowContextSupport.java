package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.entity.User;

import java.util.List;
import java.util.Map;

class ExpenseWorkflowContextSupport {

    private final AbstractExpenseWorkflowSupport support;

    ExpenseWorkflowContextSupport(AbstractExpenseWorkflowSupport support) {
        this.support = support;
    }

    Map<String, Object> buildRuntimeFlowContext(
            User currentUser,
            ProcessDocumentTemplate template,
            ProcessFormDesign formDesign,
            Map<String, Object> formData,
            ProcessExpenseDetailDesign expenseDetailDesign,
            List<ExpenseDetailInstanceDTO> expenseDetails
    ) {
        return support.buildRuntimeFlowContext(currentUser, template, formDesign, formData, expenseDetailDesign, expenseDetails);
    }

    Map<String, Object> buildRuntimeContextForInstance(ProcessDocumentInstance instance) {
        return support.buildRuntimeContextForInstance(instance);
    }

    void validateFlowSnapshot(String snapshotJson) {
        support.validateFlowSnapshot(snapshotJson);
    }

    RawFlowSnapshotSignature inspectRawFlowSnapshot(String snapshotJson) {
        return support.inspectRawFlowSnapshot(snapshotJson);
    }
}
