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
import com.finex.auth.service.queryauth.FinanceLedgerReportPermissionCodes;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import com.finex.auth.service.impl.voucher.LedgerReportDomainSupport;
import com.finex.auth.service.impl.voucher.VoucherContextSupport;
import com.finex.auth.support.FinanceModuleEnableSupport;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class FinanceLedgerReportServiceImpl implements FinanceLedgerReportService {

    private final LedgerReportDomainSupport ledgerReportDomainSupport;
    private final QueryAuthorizationService queryAuthorizationService;

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
            UserService userService,
            QueryAuthorizationService queryAuthorizationService
    ) {
        this.queryAuthorizationService = queryAuthorizationService;
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
        Set<String> visibleCompanies = new HashSet<>();
        for (String permissionCode : FinanceLedgerReportPermissionCodes.ALL_VIEWS) {
            try {
                visibleCompanies.addAll(queryAuthorizationService
                        .resolveForSelf(currentUserId, permissionCode).companyIds());
            } catch (SecurityException ignored) {
                // A missing report permission never contributes companies to the metadata list.
            }
        }
        if (visibleCompanies.isEmpty()) {
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        }
        String effectiveCompanyId = companyId == null ? "" : companyId.trim();
        if (effectiveCompanyId.isEmpty()) {
            if (visibleCompanies.size() != 1) {
                throw new IllegalArgumentException("请选择有权查看的公司");
            }
            effectiveCompanyId = visibleCompanies.iterator().next();
        }
        if (!visibleCompanies.contains(effectiveCompanyId)) {
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        }
        FinanceLedgerReportMetaVO meta = ledgerReportDomainSupport.getMeta(
                currentUserId, effectiveCompanyId, iyear, iperiod);
        meta.setCompanyOptions(meta.getCompanyOptions().stream()
                .filter(option -> visibleCompanies.contains(option.getCompanyId())).toList());
        return meta;
    }

    @Override
    public FinanceLedgerReportPageVO<FinanceBalanceSheetRowVO> queryBalanceSheet(Long currentUserId, FinanceLedgerReportQueryDTO dto) {
        requireCompany(currentUserId, FinanceLedgerReportPermissionCodes.BALANCE, dto);
        return ledgerReportDomainSupport.queryBalanceSheet(dto);
    }

    @Override
    public FinanceLedgerReportPageVO<FinanceGeneralLedgerSectionVO> queryGeneralLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto) {
        requireCompany(currentUserId, FinanceLedgerReportPermissionCodes.GENERAL, dto);
        return ledgerReportDomainSupport.queryGeneralLedger(dto);
    }

    @Override
    public FinanceLedgerReportPageVO<FinanceDetailLedgerRowVO> queryDetailLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto) {
        requireCompany(currentUserId, FinanceLedgerReportPermissionCodes.detailView(dto == null ? null : dto.getLedgerKind()), dto);
        return ledgerReportDomainSupport.queryDetailLedger(dto);
    }

    @Override
    public FinanceLedgerReportPageVO<FinanceSequenceLedgerRowVO> querySequenceLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto) {
        requireCompany(currentUserId, FinanceLedgerReportPermissionCodes.SEQUENCE, dto);
        return ledgerReportDomainSupport.querySequenceLedger(dto);
    }

    @Override
    public byte[] exportBalanceSheet(Long currentUserId, FinanceLedgerReportQueryDTO dto) {
        requireCompany(currentUserId, FinanceLedgerReportPermissionCodes.BALANCE, dto);
        return ledgerReportDomainSupport.exportBalanceSheet(dto);
    }

    @Override
    public byte[] exportGeneralLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto) {
        requireCompany(currentUserId, FinanceLedgerReportPermissionCodes.GENERAL, dto);
        return ledgerReportDomainSupport.exportGeneralLedger(dto);
    }

    @Override
    public byte[] exportDetailLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto) {
        requireCompany(currentUserId, FinanceLedgerReportPermissionCodes.detailView(dto == null ? null : dto.getLedgerKind()), dto);
        return ledgerReportDomainSupport.exportDetailLedger(dto);
    }

    @Override
    public byte[] exportSequenceLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto) {
        requireCompany(currentUserId, FinanceLedgerReportPermissionCodes.SEQUENCE, dto);
        return ledgerReportDomainSupport.exportSequenceLedger(dto);
    }

    private void requireCompany(Long userId, String permissionCode, FinanceLedgerReportQueryDTO dto) {
        if (dto == null || dto.getCompanyId() == null
                || !queryAuthorizationService.resolveForSelf(userId, permissionCode)
                        .allows(dto.getCompanyId().trim(), null)) {
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        }
    }
}
