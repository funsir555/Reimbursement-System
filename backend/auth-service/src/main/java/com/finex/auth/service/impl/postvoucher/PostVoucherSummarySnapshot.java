package com.finex.auth.service.impl.postvoucher;

final class PostVoucherSummarySnapshot {

    private final String stateStatus;
    private final int postedVoucherCount;
    private final int reviewableVoucherCount;

    PostVoucherSummarySnapshot(String stateStatus, int postedVoucherCount, int reviewableVoucherCount) {
        this.stateStatus = stateStatus;
        this.postedVoucherCount = postedVoucherCount;
        this.reviewableVoucherCount = reviewableVoucherCount;
    }

    String getStateStatus() {
        return stateStatus;
    }

    int getPostedVoucherCount() {
        return postedVoucherCount;
    }

    int getReviewableVoucherCount() {
        return reviewableVoucherCount;
    }
}
