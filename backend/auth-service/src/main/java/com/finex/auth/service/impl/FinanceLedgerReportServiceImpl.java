package com.finex.auth.service.impl;

import com.finex.auth.dto.FinanceBalanceSheetRowVO;
import com.finex.auth.dto.FinanceDetailLedgerRowVO;
import com.finex.auth.dto.FinanceGeneralLedgerSectionVO;
import com.finex.auth.dto.FinanceLedgerReportMetaVO;
import com.finex.auth.dto.FinanceLedgerReportPageVO;
import com.finex.auth.dto.FinanceLedgerReportQueryDTO;
import com.finex.auth.dto.FinanceSequenceLedgerRowVO;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSetModuleEnableMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceCashFlowItemMapper;
import com.finex.auth.mapper.FinanceCustomerMapper;
import com.finex.auth.mapper.FinancePeriodCloseMapper;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.FinanceLedgerReportService;
import com.finex.auth.service.UserService;
import com.finex.auth.service.impl.voucher.LedgerReportDomainSupport;
import com.finex.auth.service.impl.voucher.VoucherContextSupport;
import com.finex.auth.support.FinanceModuleEnableSupport;
import org.springframework.stereotype.Service;

@Service
public class FinanceLedgerReportServiceImpl implements FinanceLedgerReportService {

    private final LedgerReportDomainSupport ledgerReportDomainSupport;

    public FinanceLedgerReportServiceImpl(
            GlAccvouchMapper glAccvouchMapper,
            GlAccsumMapper glAccsumMapper,
            GlAccassMapper glAccassMapper,
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            FinanceCashFlowItemMapper financeCashFlowItemMapper,
            FinanceCustomerMapper financeCustomerMapper,
            FinanceVendorMapper financeVendorMapper,
            FinanceProjectClassMapper financeProjectClassMapper,
            FinanceProjectArchiveMapper financeProjectArchiveMapper,
            SystemCompanyMapper systemCompanyMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper,
            FinancePeriodCloseMapper financePeriodCloseMapper,
            FinanceAccountSetMapper financeAccountSetMapper,
            FinanceAccountSetModuleEnableMapper financeAccountSetModuleEnableMapper,
            UserService userService
    ) {
        FinanceModuleEnableSupport financeModuleEnableSupport =
                new FinanceModuleEnableSupport(financeAccountSetModuleEnableMapper);
        VoucherContextSupport voucherContextSupport = new VoucherContextSupport(
                systemCompanyMapper,
                financeAccountSetMapper,
                financePeriodCloseMapper,
                userService,
                financeAccountSetModuleEnableMapper
        );
        this.ledgerReportDomainSupport = new LedgerReportDomainSupport(
                glAccvouchMapper,
                glAccsumMapper,
                glAccassMapper,
                financeAccountSubjectMapper,
                financeCashFlowItemMapper,
                financeCustomerMapper,
                financeVendorMapper,
                financeProjectClassMapper,
                financeProjectArchiveMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper,
                financePeriodCloseMapper,
                financeModuleEnableSupport,
                voucherContextSupport
        );
    }

    @Override
    public FinanceLedgerReportMetaVO getMeta(Long currentUserId, String companyId, Integer iyear, Integer iperiod) {
        return ledgerReportDomainSupport.getMeta(currentUserId, companyId, iyear, iperiod);
    }

    @Override
    public FinanceLedgerReportPageVO<FinanceBalanceSheetRowVO> queryBalanceSheet(Long currentUserId, FinanceLedgerReportQueryDTO dto) {
        return ledgerReportDomainSupport.queryBalanceSheet(dto);
    }

    @Override
    public FinanceLedgerReportPageVO<FinanceGeneralLedgerSectionVO> queryGeneralLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto) {
        return ledgerReportDomainSupport.queryGeneralLedger(dto);
    }

    @Override
    public FinanceLedgerReportPageVO<FinanceDetailLedgerRowVO> queryDetailLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto) {
        return ledgerReportDomainSupport.queryDetailLedger(dto);
    }

    @Override
    public FinanceLedgerReportPageVO<FinanceSequenceLedgerRowVO> querySequenceLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto) {
        return ledgerReportDomainSupport.querySequenceLedger(dto);
    }

    @Override
    public byte[] exportBalanceSheet(Long currentUserId, FinanceLedgerReportQueryDTO dto) {
        return ledgerReportDomainSupport.exportBalanceSheet(dto);
    }

    @Override
    public byte[] exportGeneralLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto) {
        return ledgerReportDomainSupport.exportGeneralLedger(dto);
    }

    @Override
    public byte[] exportDetailLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto) {
        return ledgerReportDomainSupport.exportDetailLedger(dto);
    }

    @Override
    public byte[] exportSequenceLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto) {
        return ledgerReportDomainSupport.exportSequenceLedger(dto);
    }
}
