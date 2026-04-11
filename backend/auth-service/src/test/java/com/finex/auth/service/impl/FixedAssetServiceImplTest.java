package com.finex.auth.service.impl;

import com.finex.auth.dto.FixedAssetMetaVO;
import com.finex.auth.dto.FixedAssetVoucherLinkVO;
import com.finex.auth.service.impl.fixedasset.FixedAssetCardOpeningSupport;
import com.finex.auth.service.impl.fixedasset.FixedAssetChangeDisposalSupport;
import com.finex.auth.service.impl.fixedasset.FixedAssetDepreciationPeriodSupport;
import com.finex.auth.service.impl.fixedasset.FixedAssetMetaCategorySupport;
import com.finex.auth.service.impl.fixedasset.FixedAssetVoucherQuerySupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FixedAssetServiceImplTest {

    @Mock
    private FixedAssetMetaCategorySupport fixedAssetMetaCategorySupport;

    @Mock
    private FixedAssetCardOpeningSupport fixedAssetCardOpeningSupport;

    @Mock
    private FixedAssetChangeDisposalSupport fixedAssetChangeDisposalSupport;

    @Mock
    private FixedAssetDepreciationPeriodSupport fixedAssetDepreciationPeriodSupport;

    @Mock
    private FixedAssetVoucherQuerySupport fixedAssetVoucherQuerySupport;

    @Test
    void getMetaDelegatesToMetaSupport() {
        FixedAssetServiceImpl service = new FixedAssetServiceImpl(
                fixedAssetMetaCategorySupport,
                fixedAssetCardOpeningSupport,
                fixedAssetChangeDisposalSupport,
                fixedAssetDepreciationPeriodSupport,
                fixedAssetVoucherQuerySupport
        );
        FixedAssetMetaVO expected = new FixedAssetMetaVO();
        when(fixedAssetMetaCategorySupport.getMeta(1L, "tester", "COMPANY_A", 2026, 4)).thenReturn(expected);

        FixedAssetMetaVO result = service.getMeta(1L, "tester", "COMPANY_A", 2026, 4);

        assertSame(expected, result);
        verify(fixedAssetMetaCategorySupport).getMeta(1L, "tester", "COMPANY_A", 2026, 4);
    }

    @Test
    void getVoucherLinkDelegatesToVoucherSupport() {
        FixedAssetServiceImpl service = new FixedAssetServiceImpl(
                fixedAssetMetaCategorySupport,
                fixedAssetCardOpeningSupport,
                fixedAssetChangeDisposalSupport,
                fixedAssetDepreciationPeriodSupport,
                fixedAssetVoucherQuerySupport
        );
        FixedAssetVoucherLinkVO expected = new FixedAssetVoucherLinkVO();
        expected.setBusinessId(99L);
        when(fixedAssetVoucherQuerySupport.getVoucherLink("COMPANY_A", "DEPRECIATION_RUN", 99L)).thenReturn(expected);

        FixedAssetVoucherLinkVO result = service.getVoucherLink("COMPANY_A", "DEPRECIATION_RUN", 99L);

        assertSame(expected, result);
        verify(fixedAssetVoucherQuerySupport).getVoucherLink("COMPANY_A", "DEPRECIATION_RUN", 99L);
    }
}
