package com.finex.auth.service.impl.postvoucher;

import com.finex.auth.dto.FinancePostVoucherMetaVO;

public class PostVoucherMetaSupport {

    private final SharedPostVoucherSupport support;

    public PostVoucherMetaSupport(SharedPostVoucherSupport support) {
        this.support = support;
    }

    public FinancePostVoucherMetaVO getMeta(Long currentUserId, String companyId, Integer iyear, Integer iperiod) {
        return support.buildMeta(currentUserId, companyId, iyear, iperiod);
    }
}
