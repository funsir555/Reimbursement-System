package com.finex.auth.service.impl.financesystem;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.SystemCompanyMapper;

public final class FinanceModuleCompanyContextSupport {

    private static final String ACCOUNT_SET_STATUS_ACTIVE = "ACTIVE";

    private final SystemCompanyMapper systemCompanyMapper;
    private final FinanceAccountSetMapper financeAccountSetMapper;

    public FinanceModuleCompanyContextSupport(
            SystemCompanyMapper systemCompanyMapper,
            FinanceAccountSetMapper financeAccountSetMapper
    ) {
        this.systemCompanyMapper = systemCompanyMapper;
        this.financeAccountSetMapper = financeAccountSetMapper;
    }

    public SystemCompany requireEnabledCompany(String companyId) {
        String normalizedCompanyId = trimToNull(companyId);
        if (normalizedCompanyId == null) {
            throw new IllegalArgumentException("公司主体不能为空");
        }
        SystemCompany company = systemCompanyMapper.selectOne(
                Wrappers.<SystemCompany>lambdaQuery()
                        .eq(SystemCompany::getCompanyId, normalizedCompanyId)
                        .eq(SystemCompany::getStatus, 1)
                        .last("limit 1")
        );
        if (company == null) {
            throw new IllegalStateException("当前公司不存在或已停用");
        }
        return company;
    }

    public void requireActiveAccountSet(String companyId) {
        FinanceAccountSet accountSet = financeAccountSetMapper.selectOne(
                Wrappers.<FinanceAccountSet>lambdaQuery()
                        .eq(FinanceAccountSet::getCompanyId, companyId)
                        .eq(FinanceAccountSet::getStatus, ACCOUNT_SET_STATUS_ACTIVE)
                        .last("limit 1")
        );
        if (accountSet == null) {
            throw new IllegalStateException("当前公司未创建账套");
        }
    }

    public String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
