package com.finex.auth.service.impl.closeledger;

import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinancePeriodCloseLogMapper;
import com.finex.auth.mapper.FinancePeriodCloseMapper;
import com.finex.auth.mapper.FinancePostVoucherStateMapper;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.support.FinanceModuleEnableSupport;

public class SharedCloseLedgerSupport extends AbstractFinanceCloseLedgerSupport {

    public SharedCloseLedgerSupport(
            SystemCompanyMapper systemCompanyMapper,
            FinanceAccountSetMapper financeAccountSetMapper,
            FinancePostVoucherStateMapper financePostVoucherStateMapper,
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            FinancePeriodCloseMapper financePeriodCloseMapper,
            FinancePeriodCloseLogMapper financePeriodCloseLogMapper,
            GlAccvouchMapper glAccvouchMapper,
            GlAccsumMapper glAccsumMapper,
            GlAccassMapper glAccassMapper,
            UserMapper userMapper,
            FinanceModuleEnableSupport financeModuleEnableSupport
    ) {
        super(
                systemCompanyMapper,
                financeAccountSetMapper,
                financePostVoucherStateMapper,
                financeAccountSubjectMapper,
                financePeriodCloseMapper,
                financePeriodCloseLogMapper,
                glAccvouchMapper,
                glAccsumMapper,
                glAccassMapper,
                userMapper,
                financeModuleEnableSupport
        );
    }
}
