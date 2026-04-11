package com.finex.auth.service.impl.fixedasset;

import com.finex.auth.dto.FixedAssetMetaVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FixedAssetMetaCategorySupportTest {

    @Mock
    private AbstractFixedAssetSupport support;

    @Test
    void getMetaDelegatesToSharedSupport() {
        FixedAssetMetaCategorySupport metaCategorySupport = new FixedAssetMetaCategorySupport(support);

        metaCategorySupport.getMeta(1L, "tester", "COMPANY_A", 2026, 4);

        verify(support).getMeta(1L, "tester", "COMPANY_A", 2026, 4);
    }
}
