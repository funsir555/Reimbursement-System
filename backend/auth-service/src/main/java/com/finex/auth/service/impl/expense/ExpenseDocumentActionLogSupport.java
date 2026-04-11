package com.finex.auth.service.impl.expense;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
class ExpenseDocumentActionLogSupport {

    private final AbstractExpenseDocumentSupport support;

    ExpenseDocumentActionLogSupport(AbstractExpenseDocumentSupport support) {
        this.support = support;
    }

    void appendLog(
            String documentCode,
            String nodeKey,
            String nodeName,
            String actionType,
            Long operatorUserId,
            String operatorName,
            String actionComment,
            Map<String, Object> payload
    ) {
        support.appendLog(documentCode, nodeKey, nodeName, actionType, operatorUserId, operatorName, actionComment, payload);
    }
}
