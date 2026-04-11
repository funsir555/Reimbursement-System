package com.finex.auth.service.impl.fixedasset;

import com.finex.auth.dto.FixedAssetChangeBillSaveDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FixedAssetChangeDisposalSupportTest {

    @Mock
    private AbstractFixedAssetSupport support;

    @Test
    void createChangeBillDelegatesToSharedSupport() {
        FixedAssetChangeDisposalSupport changeDisposalSupport = new FixedAssetChangeDisposalSupport(support);
        FixedAssetChangeBillSaveDTO dto = new FixedAssetChangeBillSaveDTO();

        changeDisposalSupport.createChangeBill(dto, "tester");

        verify(support).createChangeBill(dto, "tester");
    }
}
