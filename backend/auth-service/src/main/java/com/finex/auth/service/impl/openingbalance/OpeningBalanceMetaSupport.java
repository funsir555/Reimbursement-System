package com.finex.auth.service.impl.openingbalance;

import com.finex.auth.dto.OpeningBalanceMetaVO;

public class OpeningBalanceMetaSupport {

    private final SharedOpeningBalanceSupport support;

    public OpeningBalanceMetaSupport(SharedOpeningBalanceSupport support) {
        this.support = support;
    }

    public OpeningBalanceMetaVO getMeta(Long currentUserId, String currentUsername, String companyId, Integer iyear, Integer iperiod) {
        return support.buildMeta(currentUserId, companyId, iyear, iperiod);
    }
}
