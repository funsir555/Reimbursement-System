package com.finex.auth.service.impl.fixedasset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FixedAssetCardOpeningSupportTest {

    @Mock
    private AbstractFixedAssetSupport support;

    @Test
    void getOpeningImportResultDelegatesToSharedSupport() {
        FixedAssetCardOpeningSupport cardOpeningSupport = new FixedAssetCardOpeningSupport(support);

        cardOpeningSupport.getOpeningImportResult(10L);

        verify(support).getOpeningImportResult(10L);
    }
}
