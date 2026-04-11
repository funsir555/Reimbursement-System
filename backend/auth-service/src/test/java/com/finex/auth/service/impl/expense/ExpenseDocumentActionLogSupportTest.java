package com.finex.auth.service.impl.expense;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentActionLogSupportTest {

    @Mock
    private AbstractExpenseDocumentSupport support;

    @Test
    void appendLogDelegatesToSharedSupport() {
        ExpenseDocumentActionLogSupport actionLogSupport = new ExpenseDocumentActionLogSupport(support);
        Map<String, Object> payload = Map.of("taskId", 1L);

        actionLogSupport.appendLog("DOC-1", "NODE-1", "Approve", "COMMENT", 1L, "tester", "ok", payload);

        verify(support).appendLog("DOC-1", "NODE-1", "Approve", "COMMENT", 1L, "tester", "ok", payload);
    }
}