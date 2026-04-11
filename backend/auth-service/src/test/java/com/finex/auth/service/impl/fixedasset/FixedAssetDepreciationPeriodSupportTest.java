package com.finex.auth.service.impl.fixedasset;

import com.finex.auth.dto.FixedAssetPeriodCloseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FixedAssetDepreciationPeriodSupportTest {

    @Mock
    private AbstractFixedAssetSupport support;

    @Test
    void closePeriodDelegatesToSharedSupport() {
        FixedAssetDepreciationPeriodSupport depreciationPeriodSupport = new FixedAssetDepreciationPeriodSupport(support);
        FixedAssetPeriodCloseDTO dto = new FixedAssetPeriodCloseDTO();

        depreciationPeriodSupport.closePeriod(dto, "tester");

        verify(support).closePeriod(dto, "tester");
    }
}
