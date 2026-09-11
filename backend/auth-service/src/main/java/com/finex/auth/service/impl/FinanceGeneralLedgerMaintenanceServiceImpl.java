package com.finex.auth.service.impl;

import com.finex.auth.dto.FinanceGeneralLedgerRollbackPeriodDTO;
import com.finex.auth.dto.FinanceGeneralLedgerRollbackPeriodResultVO;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSetModuleEnableMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinancePeriodCloseLogMapper;
import com.finex.auth.mapper.FinancePeriodCloseMapper;
import com.finex.auth.mapper.FinancePostVoucherStateMapper;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.FinanceGeneralLedgerMaintenanceService;
import com.finex.auth.service.impl.closeledger.CloseLedgerRollbackSupport;
import com.finex.auth.service.impl.closeledger.SharedCloseLedgerSupport;
import com.finex.auth.support.FinanceModuleEnableSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceGeneralLedgerMaintenanceServiceImpl implements FinanceGeneralLedgerMaintenanceService {

    private final CloseLedgerRollbackSupport closeLedgerRollbackSupport;

    public FinanceGeneralLedgerMaintenanceServiceImpl(
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
            FinanceAccountSetModuleEnableMapper financeAccountSetModuleEnableMapper
    ) {
        FinanceModuleEnableSupport financeModuleEnableSupport =
                new FinanceModuleEnableSupport(financeAccountSetModuleEnableMapper);
        SharedCloseLedgerSupport support = new SharedCloseLedgerSupport(
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
        this.closeLedgerRollbackSupport = new CloseLedgerRollbackSupport(support);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceGeneralLedgerRollbackPeriodResultVO rollbackPeriod(
            Long currentUserId,
            String operatorName,
            FinanceGeneralLedgerRollbackPeriodDTO dto
    ) {
        return closeLedgerRollbackSupport.rollbackPeriod(
                currentUserId,
                dto.getCompanyId(),
                dto.getIyear(),
                dto.getIperiod(),
                operatorName
        );
    }
}
