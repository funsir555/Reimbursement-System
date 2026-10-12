package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentInstance;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpensePaymentStatusPolicyTest {

    @Test
    void nonTerminalPaymentStatesDoNotReadActionHistory() {
        AtomicInteger reads = new AtomicInteger();
        ProcessDocumentInstance instance = instance("PAYING");

        assertEquals("PENDING_PAYMENT", ExpensePaymentStatusPolicy.resolveVoidTargetStatus(
                instance, code -> {
                    reads.incrementAndGet();
                    return List.of();
                }));
        instance.setStatus("PENDING_APPROVAL");
        assertEquals("PENDING_PAYMENT", ExpensePaymentStatusPolicy.resolveVoidTargetStatus(
                instance, code -> {
                    reads.incrementAndGet();
                    return List.of();
                }));
        assertEquals("PENDING_PAYMENT", ExpensePaymentStatusPolicy.resolveVoidTargetStatus(
                null, code -> {
                    reads.incrementAndGet();
                    return List.of();
                }));
        assertEquals(0, reads.get());
    }

    @Test
    void voidDecisionUsesNearestRelevantActionBeforeLatestMatchingAnchor() {
        ProcessDocumentInstance instance = instance("PAYMENT_COMPLETED");
        List<ProcessDocumentActionLog> previousStart = List.of(
                action("PAYMENT_PENDING"), action("PAYMENT_START"), action("IGNORED"), action("PAYMENT_COMPLETE"));
        assertEquals("PAYING", ExpensePaymentStatusPolicy.resolveVoidTargetStatus(
                instance, code -> previousStart));

        List<ProcessDocumentActionLog> previousEnd = List.of(
                action("PAYMENT_START"), action("PAYMENT_COMPLETE"), action("PAYMENT_PENDING"),
                action("PAYMENT_COMPLETE"));
        assertEquals("PENDING_PAYMENT", ExpensePaymentStatusPolicy.resolveVoidTargetStatus(
                instance, code -> previousEnd));

        instance.setStatus("PAYMENT_EXCEPTION");
        assertEquals("PENDING_PAYMENT", ExpensePaymentStatusPolicy.resolveVoidTargetStatus(
                instance, code -> List.of(action("PAYMENT_COMPLETE"))));
        assertEquals("PENDING_PAYMENT", ExpensePaymentStatusPolicy.resolveVoidTargetStatus(
                instance, code -> null));
    }

    @Test
    void revertOnlyAcceptsPayingAsExplicitTarget() {
        assertEquals("PAYING", ExpensePaymentStatusPolicy.normalizeRevertTargetStatus(" PAYING "));
        assertEquals("PENDING_PAYMENT", ExpensePaymentStatusPolicy.normalizeRevertTargetStatus("PAYMENT_COMPLETED"));
        assertEquals("PENDING_PAYMENT", ExpensePaymentStatusPolicy.normalizeRevertTargetStatus(null));
    }

    private static ProcessDocumentInstance instance(String status) {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-PAYMENT");
        instance.setStatus(status);
        return instance;
    }

    private static ProcessDocumentActionLog action(String type) {
        ProcessDocumentActionLog log = new ProcessDocumentActionLog();
        log.setActionType(type);
        return log;
    }
}
