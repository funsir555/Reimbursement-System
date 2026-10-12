package com.finex.auth.service.impl.voucher;

import com.finex.auth.entity.GlAccvouch;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoucherRedReversalTest {
    private final VoucherMutationDomainSupport vouchers = new VoucherMutationDomainSupport(
            null, null, null, null, null, null, null, null, null, null, null, null);

    @Test
    void reversalNegatesAmountsAndQuantitiesInTheirOriginalColumns() {
        GlAccvouch source = new GlAccvouch();
        source.setCompanyId("A");
        source.setIyear(2022);
        source.setIperiod(12);
        source.setCsign("记");
        source.setInoId(19);
        source.setMd(new BigDecimal("2273513.79"));
        source.setMc(BigDecimal.ZERO);
        source.setMdF(new BigDecimal("100.00"));
        source.setMcF(BigDecimal.ZERO);
        source.setNdS(new BigDecimal("4.500000"));
        source.setNcS(null);

        GlAccvouch reversed = vouchers.cloneVoucherRowForReverse(source, 20, 1, 1, 1L, "tester");
        assertEquals(new BigDecimal("-2273513.79"), reversed.getMd());
        assertEquals(new BigDecimal("0.00"), reversed.getMc());
        assertEquals(new BigDecimal("-100.00"), reversed.getMdF());
        assertEquals(new BigDecimal("0.00"), reversed.getMcF());
        assertEquals(new BigDecimal("-4.500000"), reversed.getNdS());
        assertEquals(null, reversed.getNcS());
        assertEquals("A~2022~12~记~19", reversed.getReversedFromVoucherNo());

        GlAccvouch restored = vouchers.cloneVoucherRowForReverse(reversed, 21, 1, 1, 1L, "tester");
        assertEquals(source.getMd(), restored.getMd());
        assertEquals(source.getMdF(), restored.getMdF());
        assertEquals(source.getNdS(), restored.getNdS());
    }
}
