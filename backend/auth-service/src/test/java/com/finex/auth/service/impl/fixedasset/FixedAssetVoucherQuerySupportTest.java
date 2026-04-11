package com.finex.auth.service.impl.fixedasset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FixedAssetVoucherQuerySupportTest {

    @Mock
    private AbstractFixedAssetSupport support;

    @Test
    void getVoucherLinkDelegatesToSharedSupport() {
        FixedAssetVoucherQuerySupport voucherQuerySupport = new FixedAssetVoucherQuerySupport(support);

        voucherQuerySupport.getVoucherLink("COMPANY_A", "DEPRECIATION_RUN", 8L);

        verify(support).getVoucherLink("COMPANY_A", "DEPRECIATION_RUN", 8L);
    }
}
