package com.finex.auth.service.impl;

import com.finex.auth.dto.FinanceGeneralLedgerPeriodActionDTO;
import com.finex.auth.dto.FinanceGeneralLedgerPeriodActionResultVO;
import com.finex.auth.dto.FinanceGeneralLedgerPeriodStatusOverviewVO;
import com.finex.auth.entity.User;
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
import com.finex.auth.service.FinanceGeneralLedgerPeriodStatusService;
import com.finex.auth.service.FinancePeriodTransferService;
import com.finex.auth.service.impl.closeledger.GeneralLedgerPeriodStatusSupport;
import com.finex.auth.service.impl.closeledger.SharedCloseLedgerSupport;
import com.finex.auth.support.FinanceModuleEnableSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceGeneralLedgerPeriodStatusServiceImpl implements FinanceGeneralLedgerPeriodStatusService {

    private final GeneralLedgerPeriodStatusSupport generalLedgerPeriodStatusSupport;

    public FinanceGeneralLedgerPeriodStatusServiceImpl(
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
            FinanceAccountSetModuleEnableMapper financeAccountSetModuleEnableMapper,
            FinancePeriodTransferService financePeriodTransferService
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
        this.generalLedgerPeriodStatusSupport = new GeneralLedgerPeriodStatusSupport(
                support,
                new GeneralLedgerPeriodStatusSupport.UserMapperBridge() {
                    @Override
                    public User loadUser(Long userId) {
                        return userId == null ? null : userMapper.selectById(userId);
                    }
                },
                financePeriodTransferService
        );
    }

    @Override
    public FinanceGeneralLedgerPeriodStatusOverviewVO getOverview(
            Long currentUserId,
            String companyId,
            Integer iyear,
            Integer currentIyear,
            Integer currentIperiod
    ) {
        return generalLedgerPeriodStatusSupport.getOverview(currentUserId, companyId, iyear, currentIyear, currentIperiod);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceGeneralLedgerPeriodActionResultVO reopenPeriod(
            Long currentUserId,
            String currentUsername,
            FinanceGeneralLedgerPeriodActionDTO dto
    ) {
        return generalLedgerPeriodStatusSupport.reopenPeriod(
                currentUserId,
                currentUsername,
                dto.getCompanyId(),
                dto.getIyear(),
                dto.getIperiod(),
                dto.getCurrentIyear(),
                dto.getCurrentIperiod(),
                dto.getPassword()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceGeneralLedgerPeriodActionResultVO unpostPeriod(
            Long currentUserId,
            String currentUsername,
            FinanceGeneralLedgerPeriodActionDTO dto
    ) {
        return generalLedgerPeriodStatusSupport.unpostPeriod(
                currentUserId,
                currentUsername,
                dto.getCompanyId(),
                dto.getIyear(),
                dto.getIperiod(),
                dto.getCurrentIyear(),
                dto.getCurrentIperiod(),
                dto.getPassword()
        );
    }
}
