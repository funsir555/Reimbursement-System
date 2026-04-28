package com.finex.auth.service.impl.postvoucher;

import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceOpeningBalanceStateMapper;
import com.finex.auth.mapper.FinancePeriodCloseMapper;
import com.finex.auth.mapper.FinancePostVoucherStateMapper;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;

public final class SharedPostVoucherSupport extends AbstractFinancePostVoucherSupport {

    public SharedPostVoucherSupport(
            FinanceAccountSetMapper financeAccountSetMapper,
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            FinanceOpeningBalanceStateMapper financeOpeningBalanceStateMapper,
            FinancePostVoucherStateMapper financePostVoucherStateMapper,
            FinancePeriodCloseMapper financePeriodCloseMapper,
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            GlAccvouchMapper glAccvouchMapper,
            GlAccsumMapper glAccsumMapper,
            GlAccassMapper glAccassMapper,
            SystemCompanyMapper systemCompanyMapper,
            UserMapper userMapper
    ) {
        super(
                financeAccountSetMapper,
                financeAccountSubjectMapper,
                financeOpeningBalanceStateMapper,
                financePostVoucherStateMapper,
                financePeriodCloseMapper,
                asyncTaskRecordMapper,
                glAccvouchMapper,
                glAccsumMapper,
                glAccassMapper,
                systemCompanyMapper,
                userMapper
        );
    }
}
