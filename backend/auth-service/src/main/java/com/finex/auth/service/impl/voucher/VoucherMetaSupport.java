// 业务域：财务凭证
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 凭证查询、新建、修改等接口，下游会继续协调 凭证主表、分录、上下文数据与报销关联。
// 风险提醒：改坏后最容易影响 凭证金额、分录科目和与单据的对应关系。

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

/**
 * VoucherMetaSupport：通用支撑类。
 * 封装 凭证这块可复用的业务能力。
 * 改这里时，要特别关注 凭证金额、分录科目和与单据的对应关系是否会被一起带坏。
 */
public final class VoucherMetaSupport extends AbstractFinanceVoucherSupport {

    private final VoucherContextSupport voucherContextSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
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

    /**
     * 获取元数据。
     */
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

    /**
     * 解析Effective公司Id。
     */
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
