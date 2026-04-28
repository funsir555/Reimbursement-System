package com.finex.auth.service.impl.postvoucher;

import com.finex.auth.entity.FinancePostVoucherState;
import com.finex.auth.entity.GlAccvouch;
import java.util.List;
import java.util.Map;

final class PostVoucherRunContext {

    private final String companyId;
    private final String companyName;
    private final int iyear;
    private final int iperiod;
    private final int iyperiod;
    private final List<Map.Entry<AbstractFinancePostVoucherSupport.VoucherKey, List<GlAccvouch>>> reviewableVoucherEntries;
    private final int postedVoucherCount;
    private final FinancePostVoucherState state;

    PostVoucherRunContext(
            String companyId,
            String companyName,
            int iyear,
            int iperiod,
            int iyperiod,
            List<Map.Entry<AbstractFinancePostVoucherSupport.VoucherKey, List<GlAccvouch>>> reviewableVoucherEntries,
            int postedVoucherCount,
            FinancePostVoucherState state
    ) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.iyear = iyear;
        this.iperiod = iperiod;
        this.iyperiod = iyperiod;
        this.reviewableVoucherEntries = reviewableVoucherEntries;
        this.postedVoucherCount = postedVoucherCount;
        this.state = state;
    }

    String getCompanyId() {
        return companyId;
    }

    String getCompanyName() {
        return companyName;
    }

    int getIyear() {
        return iyear;
    }

    int getIperiod() {
        return iperiod;
    }

    int getIyperiod() {
        return iyperiod;
    }

    List<Map.Entry<AbstractFinancePostVoucherSupport.VoucherKey, List<GlAccvouch>>> getReviewableVoucherEntries() {
        return reviewableVoucherEntries;
    }

    int getPostedVoucherCount() {
        return postedVoucherCount;
    }

    FinancePostVoucherState getState() {
        return state;
    }
}
