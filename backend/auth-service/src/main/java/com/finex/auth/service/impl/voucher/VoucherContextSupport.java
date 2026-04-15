// 业务域：财务凭证
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 凭证查询、新建、修改等接口，下游会继续协调 凭证主表、分录、上下文数据与报销关联。
// 风险提醒：改坏后最容易影响 凭证金额、分录科目和与单据的对应关系。

package com.finex.auth.service.impl.voucher;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.FinanceContextCompanyOptionVO;
import com.finex.auth.dto.FinanceContextMetaVO;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.service.UserService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * VoucherContextSupport：通用支撑类。
 * 封装 凭证这块可复用的业务能力。
 * 改这里时，要特别关注 凭证金额、分录科目和与单据的对应关系是否会被一起带坏。
 */
public final class VoucherContextSupport {

    private static final String ACCOUNT_SET_STATUS_ACTIVE = "ACTIVE";

    private final SystemCompanyMapper systemCompanyMapper;
    private final FinanceAccountSetMapper financeAccountSetMapper;
    private final UserService userService;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public VoucherContextSupport(
            SystemCompanyMapper systemCompanyMapper,
            FinanceAccountSetMapper financeAccountSetMapper,
            UserService userService
    ) {
        this.systemCompanyMapper = systemCompanyMapper;
        this.financeAccountSetMapper = financeAccountSetMapper;
        this.userService = userService;
    }

    /**
     * 获取元数据。
     */
    public FinanceContextMetaVO getMeta(Long currentUserId) {
        List<SystemCompany> companies = loadEnabledCompanies();
        Set<String> activeAccountSetCompanyIds = loadActiveAccountSetCompanyIds();

        FinanceContextMetaVO meta = new FinanceContextMetaVO();
        meta.setCompanyOptions(companies.stream()
                .map(company -> toOption(company, activeAccountSetCompanyIds.contains(company.getCompanyId())))
                .toList());

        User currentUser = currentUserId == null ? null : userService.getById(currentUserId);
        String rawCurrentUserCompanyId = normalize(currentUser == null ? null : currentUser.getCompanyId());
        String resolvedCurrentUserCompanyId = meta.getCompanyOptions().stream()
                .map(FinanceContextCompanyOptionVO::getCompanyId)
                .filter(item -> item.equals(rawCurrentUserCompanyId))
                .findFirst()
                .orElse(null);
        meta.setCurrentUserCompanyId(resolvedCurrentUserCompanyId);
        String defaultCompanyId = meta.getCompanyOptions().stream()
                .filter(FinanceContextCompanyOptionVO::isHasActiveAccountSet)
                .map(FinanceContextCompanyOptionVO::getCompanyId)
                .findFirst()
                .orElse(resolvedCurrentUserCompanyId != null
                        ? resolvedCurrentUserCompanyId
                        : (meta.getCompanyOptions().isEmpty() ? null : meta.getCompanyOptions().get(0).getCompanyId()));
        meta.setDefaultCompanyId(defaultCompanyId);
        return meta;
    }

    /**
     * 加载EnabledCompanies。
     */
    public List<SystemCompany> loadEnabledCompanies() {
        return systemCompanyMapper.selectList(
                Wrappers.<SystemCompany>lambdaQuery()
                        .eq(SystemCompany::getStatus, 1)
                        .orderByAsc(SystemCompany::getCompanyCode, SystemCompany::getCompanyId)
        );
    }

    /**
     * 加载Active账户Set公司Ids。
     */
    private Set<String> loadActiveAccountSetCompanyIds() {
        return financeAccountSetMapper.selectList(
                        Wrappers.<FinanceAccountSet>lambdaQuery()
                                .eq(FinanceAccountSet::getStatus, ACCOUNT_SET_STATUS_ACTIVE)
                                .orderByAsc(FinanceAccountSet::getCompanyId)
                ).stream()
                .map(FinanceAccountSet::getCompanyId)
                .filter(item -> normalize(item) != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private FinanceContextCompanyOptionVO toOption(SystemCompany company, boolean hasActiveAccountSet) {
        FinanceContextCompanyOptionVO option = new FinanceContextCompanyOptionVO();
        option.setCompanyId(company.getCompanyId());
        option.setCompanyCode(company.getCompanyCode());
        option.setCompanyName(company.getCompanyName());
        option.setHasActiveAccountSet(hasActiveAccountSet);
        option.setValue(company.getCompanyId());
        option.setLabel(normalize(company.getCompanyCode()) == null
                ? company.getCompanyName()
                : company.getCompanyCode() + " - " + company.getCompanyName());
        return option;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
