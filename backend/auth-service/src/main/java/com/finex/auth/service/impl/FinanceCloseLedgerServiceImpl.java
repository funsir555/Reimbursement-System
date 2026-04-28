package com.finex.auth.service.impl;

import com.finex.auth.dto.FinanceCloseLedgerMetaVO;
import com.finex.auth.dto.FinanceCloseLedgerReconcileResultVO;
import com.finex.auth.dto.FinanceCloseLedgerRequestDTO;
import com.finex.auth.dto.FinanceCloseLedgerValidationResultVO;
import com.finex.auth.mapper.FaAssetPeriodCloseMapper;
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
import com.finex.auth.service.FinanceCloseLedgerService;
import com.finex.auth.service.impl.closeledger.CloseLedgerExternalCheckerRegistry;
import com.finex.auth.service.impl.closeledger.CloseLedgerMetaSupport;
import com.finex.auth.service.impl.closeledger.CloseLedgerMutationSupport;
import com.finex.auth.service.impl.closeledger.CloseLedgerReconcileSupport;
import com.finex.auth.service.impl.closeledger.CloseLedgerValidationSupport;
import com.finex.auth.service.impl.closeledger.FixedAssetPeriodCloseChecker;
import com.finex.auth.service.impl.closeledger.SharedCloseLedgerSupport;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceCloseLedgerServiceImpl implements FinanceCloseLedgerService {

    private final CloseLedgerMetaSupport closeLedgerMetaSupport;
    private final CloseLedgerReconcileSupport closeLedgerReconcileSupport;
    private final CloseLedgerValidationSupport closeLedgerValidationSupport;
    private final CloseLedgerMutationSupport closeLedgerMutationSupport;

    public FinanceCloseLedgerServiceImpl(
            SystemCompanyMapper systemCompanyMapper,
            FinanceAccountSetMapper financeAccountSetMapper,
            FinancePostVoucherStateMapper financePostVoucherStateMapper,
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            FinancePeriodCloseMapper financePeriodCloseMapper,
            FinancePeriodCloseLogMapper financePeriodCloseLogMapper,
            GlAccvouchMapper glAccvouchMapper,
            GlAccsumMapper glAccsumMapper,
            GlAccassMapper glAccassMapper,
            FaAssetPeriodCloseMapper faAssetPeriodCloseMapper,
            UserMapper userMapper
    ) {
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
                userMapper
        );
        CloseLedgerExternalCheckerRegistry externalCheckerRegistry = new CloseLedgerExternalCheckerRegistry(
                List.of(new FixedAssetPeriodCloseChecker(faAssetPeriodCloseMapper))
        );
        this.closeLedgerMetaSupport = new CloseLedgerMetaSupport(support, externalCheckerRegistry);
        this.closeLedgerReconcileSupport = new CloseLedgerReconcileSupport(support);
        this.closeLedgerValidationSupport = new CloseLedgerValidationSupport(support, closeLedgerReconcileSupport, externalCheckerRegistry);
        this.closeLedgerMutationSupport = new CloseLedgerMutationSupport(support, closeLedgerReconcileSupport, closeLedgerValidationSupport);
    }

    @Override
    public FinanceCloseLedgerMetaVO getMeta(Long currentUserId, String companyId, Integer iyear, Integer iperiod) {
        return closeLedgerMetaSupport.getMeta(currentUserId, companyId, iyear, iperiod);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceCloseLedgerReconcileResultVO reconcile(Long currentUserId, String operatorName, FinanceCloseLedgerRequestDTO dto) {
        return closeLedgerReconcileSupport.reconcile(
                currentUserId,
                dto.getCompanyId(),
                dto.getIyear(),
                dto.getIperiod(),
                closeLedgerMutationSupport.resolveOperatorName(currentUserId, operatorName)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceCloseLedgerValidationResultVO validate(Long currentUserId, String operatorName, FinanceCloseLedgerRequestDTO dto) {
        return closeLedgerValidationSupport.validate(
                currentUserId,
                dto.getCompanyId(),
                dto.getIyear(),
                dto.getIperiod(),
                closeLedgerMutationSupport.resolveOperatorName(currentUserId, operatorName)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceCloseLedgerMetaVO close(Long currentUserId, String operatorName, FinanceCloseLedgerRequestDTO dto) {
        return closeLedgerMutationSupport.close(
                currentUserId,
                dto.getCompanyId(),
                dto.getIyear(),
                dto.getIperiod(),
                dto.getCloseNote(),
                closeLedgerMutationSupport.resolveOperatorName(currentUserId, operatorName)
        );
    }
}
