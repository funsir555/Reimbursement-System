package com.finex.auth.service.impl.fixedasset;

import com.finex.auth.dto.FixedAssetVoucherLinkVO;

public class FixedAssetVoucherQuerySupport {

    private final AbstractFixedAssetSupport support;

    public FixedAssetVoucherQuerySupport(AbstractFixedAssetSupport support) {
        this.support = support;
    }

    public FixedAssetVoucherLinkVO getVoucherLink(String companyId, String businessType, Long businessId) {
        return support.getVoucherLink(companyId, businessType, businessId);
    }
}
