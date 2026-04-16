// 业务域：财务档案
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service.impl;

import com.finex.auth.dto.FinanceBankBranchVO;
import com.finex.auth.dto.FinanceBankOptionVO;
import com.finex.auth.service.FinanceBankCatalogService;
import com.finex.auth.service.bankcatalog.BankCatalogProvider;
import com.finex.auth.service.bankcatalog.BankCatalogProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * FinanceBankCatalogServiceImpl：service 入口实现。
 * 接住上层请求，并把 财务银行目录相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
@Service
@RequiredArgsConstructor
public class FinanceBankCatalogServiceImpl implements FinanceBankCatalogService {

    private final List<BankCatalogProvider> providers;

    /**
     * 查询银行列表。
     */
    @Override
    public List<FinanceBankOptionVO> listBanks(String keyword, String businessScope) {
        return activeProvider().listBanks(keyword, businessScope);
    }

    @Override
    public List<String> listProvinces(String bankCode, String businessScope) {
        return activeProvider().listProvinces(bankCode, businessScope);
    }

    @Override
    public List<String> listCities(String bankCode, String province, String businessScope) {
        return activeProvider().listCities(bankCode, province, businessScope);
    }

    /**
     * 查询银行Branches列表。
     */
    @Override
    public List<FinanceBankBranchVO> listBankBranches(String bankCode, String province, String city, String keyword, String businessScope) {
        return activeProvider().listBankBranches(bankCode, province, city, keyword, businessScope);
    }

    /**
     * 处理财务银行目录中的这一步。
     */
    @Override
    public FinanceBankBranchVO lookupBranchByCnaps(String cnapsCode) {
        return activeProvider().lookupBranchByCnaps(cnapsCode);
    }

    private BankCatalogProvider activeProvider() {
        return providers.stream()
                .filter(provider -> provider.getType() == BankCatalogProviderType.LOCAL)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到本地银行目录提供者"));
    }
}
