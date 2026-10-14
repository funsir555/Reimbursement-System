package com.finex.auth.agent.workflow;

import com.finex.auth.service.port.FinanceAgentQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorization;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SupplierLedgerSelectionTest {
    private final FinanceAgentQueryPort finance = mock(FinanceAgentQueryPort.class);
    private final QueryAuthorization grant = mock(QueryAuthorization.class);

    @Test
    void fullArchiveNameWinsOverPartialNumberAndModelCodeIsIgnored() {
        var supplier = new FinanceAgentQueryPort.Supplier("40036", "供应商6");
        when(finance.suppliers(grant, "C1", "供应商6")).thenReturn(List.of(supplier));

        var match = SupplierLedgerSelection.resolve(finance, grant, "C1",
                "查询黄埔远智跟供应商6所有往来明细", "供应商6", null);

        assertEquals("40036", match.code());
    }

    @Test
    void ambiguousNameRequiresAuthorizedChoiceAndRechecksTheCode() {
        var first = new FinanceAgentQueryPort.Supplier("V1", "甲公司");
        var second = new FinanceAgentQueryPort.Supplier("V2", "甲公司");
        when(finance.suppliers(grant, "C1", "甲公司")).thenReturn(List.of(first, second));
        when(finance.suppliers(grant, "C1", "V2")).thenReturn(List.of(second));

        var unresolved = SupplierLedgerSelection.resolve(finance, grant, "C1",
                "查询供应商：甲公司的明细", null, null);
        assertFalse(unresolved.resolved());
        assertEquals(List.of(first, second), unresolved.options());
        assertEquals("V2", SupplierLedgerSelection.resolve(finance, grant, "C1",
                "查询供应商：甲公司的明细", null, "V2").code());
        assertThrows(IllegalArgumentException.class, () -> SupplierLedgerSelection.resolve(finance,
                grant, "C1", "查询供应商：甲公司的明细", null, "INVENTED"));
    }

    @Test
    void nameWithoutAuthorizedMatchNeedsClarification() {
        var match = SupplierLedgerSelection.resolve(finance, grant, "C1",
                "查询供应商：不存在的明细", null, null);
        assertFalse(match.resolved());
        assertEquals(List.of(), match.options());
    }

    @Test
    void aClarifiedSupplierNameReplacesAnEarlierUnmatchedName() {
        var supplier = new FinanceAgentQueryPort.Supplier("40036", "供应商6");
        when(finance.suppliers(grant, "C1", "供应商6")).thenReturn(List.of(supplier));

        var match = SupplierLedgerSelection.resolve(finance, grant, "C1",
                "查询供应商：不存在的明细。补充：供应商6", null, null);

        assertEquals("40036", match.code());
    }

    @Test
    void aPlainNameHintIsResolvedOnlyThroughTheAuthorizedArchive() {
        var supplier = new FinanceAgentQueryPort.Supplier("V1", "云之智");
        when(finance.suppliers(grant, "C1", "云之智")).thenReturn(List.of(supplier));

        var match = SupplierLedgerSelection.resolve(finance, grant, "C1",
                "查询供应商明细账 云之智", "云之智", null);

        assertEquals("V1", match.code());
    }

    @Test
    void latestClarificationCanReplaceAnEarlierValidSupplier() {
        var replacement = new FinanceAgentQueryPort.Supplier("V2", "云之智");
        when(finance.suppliers(grant, "C1", "云之智")).thenReturn(List.of(replacement));

        var match = SupplierLedgerSelection.resolve(finance, grant, "C1",
                "查询供应商6的明细\n补充说明：改成云之智", "云之智", null);

        assertEquals("V2", match.code());
    }
}
