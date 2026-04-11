package com.finex.auth.service.impl.voucher;

import com.finex.auth.dto.FinanceContextCompanyOptionVO;
import com.finex.auth.dto.FinanceContextMetaVO;
import com.finex.auth.dto.FinanceVoucherMetaVO;
import com.finex.auth.dto.FinanceVoucherOptionVO;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceCustomerMapper;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

public final class VoucherMetaSupport extends AbstractFinanceVoucherSupport {

    private final VoucherContextSupport voucherContextSupport;

    public VoucherMetaSupport(
            GlAccvouchMapper glAccvouchMapper,
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            FinanceCustomerMapper financeCustomerMapper,
            FinanceVendorMapper financeVendorMapper,
            FinanceProjectClassMapper financeProjectClassMapper,
            FinanceProjectArchiveMapper financeProjectArchiveMapper,
            SystemCompanyMapper systemCompanyMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper,
            VoucherContextSupport voucherContextSupport
    ) {
        super(glAccvouchMapper, financeAccountSubjectMapper, financeCustomerMapper, financeVendorMapper, financeProjectClassMapper, financeProjectArchiveMapper, systemCompanyMapper, systemDepartmentMapper, userMapper);
        this.voucherContextSupport = voucherContextSupport;
    }

    public FinanceVoucherMetaVO getMeta(
            Long currentUserId,
            String currentUsername,
            String companyId,
            String billDate,
            String csign
    ) {
        User currentUser = requireUser(currentUserId);
        FinanceContextMetaVO contextMeta = voucherContextSupport.getMeta(currentUserId);
        String effectiveCompanyId = resolveEffectiveCompanyId(companyId, contextMeta);
        LocalDate effectiveBillDate = parseDateOrDefault(billDate, LocalDate.now());
        String effectiveVoucherType = normalize(csign, DEFAULT_VOUCHER_TYPE);

        FinanceVoucherMetaVO meta = new FinanceVoucherMetaVO();
        meta.setCompanyOptions(contextMeta.getCompanyOptions().stream().map(this::toVoucherCompanyOption).toList());
        meta.setDepartmentOptions(loadEnabledDepartments().stream().map(this::toDepartmentOption).toList());
        meta.setEmployeeOptions(loadEnabledUsers().stream().map(this::toEmployeeOption).toList());
        meta.setVoucherTypeOptions(toOptions(VOUCHER_TYPE_SEEDS));
        meta.setCurrencyOptions(toOptions(CURRENCY_SEEDS));
        meta.setAccountOptions(loadAccountOptions(effectiveCompanyId));
        meta.setCustomerOptions(loadCustomerOptions(effectiveCompanyId));
        meta.setSupplierOptions(loadSupplierOptions(effectiveCompanyId));
        meta.setProjectClassOptions(loadProjectClassOptions(effectiveCompanyId));
        meta.setProjectOptions(loadProjectOptions(effectiveCompanyId));
        meta.setDefaultCompanyId(effectiveCompanyId);
        meta.setDefaultBillDate(effectiveBillDate.format(DATE_FORMATTER));
        meta.setDefaultPeriod(effectiveBillDate.getMonthValue());
        meta.setDefaultVoucherType(effectiveVoucherType);
        meta.setSuggestedVoucherNo(nextVoucherNo(effectiveCompanyId, effectiveBillDate.getMonthValue(), effectiveVoucherType));
        meta.setDefaultMaker(resolveMakerName(currentUser, currentUsername));
        meta.setDefaultAttachedDocCount(0);
        meta.setDefaultCurrency(DEFAULT_CURRENCY);
        return meta;
    }

    private String resolveEffectiveCompanyId(String companyId, FinanceContextMetaVO contextMeta) {
        String normalizedCompanyId = trimToNull(companyId);
        Set<String> availableCompanyIds = contextMeta.getCompanyOptions().stream()
                .map(FinanceContextCompanyOptionVO::getCompanyId)
                .filter(item -> trimToNull(item) != null)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        if (normalizedCompanyId != null && availableCompanyIds.contains(normalizedCompanyId)) {
            return normalizedCompanyId;
        }
        return contextMeta.getDefaultCompanyId();
    }

    private FinanceVoucherOptionVO toVoucherCompanyOption(FinanceContextCompanyOptionVO option) {
        FinanceVoucherOptionVO companyOption = new FinanceVoucherOptionVO();
        companyOption.setValue(option.getCompanyId());
        companyOption.setCode(option.getCompanyCode());
        companyOption.setName(option.getCompanyName());
        companyOption.setLabel(option.getLabel());
        return companyOption;
    }
}
