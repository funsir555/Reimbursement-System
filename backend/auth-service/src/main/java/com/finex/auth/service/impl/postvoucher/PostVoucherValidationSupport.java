package com.finex.auth.service.impl.postvoucher;

public class PostVoucherValidationSupport {

    private final SharedPostVoucherSupport support;

    public PostVoucherValidationSupport(SharedPostVoucherSupport support) {
        this.support = support;
    }

    public PostVoucherRunContext prepareRun(String companyId, Integer iyear, Integer iperiod) {
        return support.prepareRun(companyId, iyear, iperiod);
    }

    public String resolveOperatorName(Long currentUserId, String fallbackUsername) {
        return support.resolveOperatorName(currentUserId, fallbackUsername);
    }
}
