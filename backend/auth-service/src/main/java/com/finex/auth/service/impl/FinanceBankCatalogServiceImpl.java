// 业务域：财务档案
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finex.auth.dto.FinanceBankBranchVO;
import com.finex.auth.dto.FinanceBankOptionVO;
import com.finex.auth.entity.SystemBankBranchCatalog;
import com.finex.auth.entity.SystemBankCatalog;
import com.finex.auth.mapper.SystemBankBranchCatalogMapper;
import com.finex.auth.mapper.SystemBankCatalogMapper;
import com.finex.auth.service.FinanceBankCatalogService;
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

    private final SystemBankCatalogMapper systemBankCatalogMapper;
    private final SystemBankBranchCatalogMapper systemBankBranchCatalogMapper;

    /**
     * 查询银行列表。
     */
    @Override
    public List<FinanceBankOptionVO> listBanks(String keyword) {
        QueryWrapper<SystemBankCatalog> query = new QueryWrapper<>();
        String normalizedKeyword = trimToNull(keyword);
        query.eq("status", 1);
        if (normalizedKeyword != null) {
            query.and(wrapper -> wrapper
                    .like("bank_code", normalizedKeyword)
                    .or()
                    .like("bank_name", normalizedKeyword));
        }
        query.orderByAsc("sort_order").orderByAsc("bank_code").last("limit 80");
        return systemBankCatalogMapper.selectList(query).stream().map(this::toBankOption).toList();
    }

    /**
     * 查询银行Branches列表。
     */
    @Override
    public List<FinanceBankBranchVO> listBankBranches(String bankCode, String province, String city, String keyword) {
        QueryWrapper<SystemBankBranchCatalog> query = new QueryWrapper<>();
        String normalizedBankCode = trimToNull(bankCode);
        String normalizedProvince = trimToNull(province);
        String normalizedCity = trimToNull(city);
        String normalizedKeyword = trimToNull(keyword);
        query.eq("status", 1);
        if (normalizedBankCode != null) {
            query.eq("bank_code", normalizedBankCode);
        }
        if (normalizedProvince != null) {
            query.eq("province", normalizedProvince);
        }
        if (normalizedCity != null) {
            query.eq("city", normalizedCity);
        }
        if (normalizedKeyword != null) {
            query.and(wrapper -> wrapper
                    .like("branch_name", normalizedKeyword)
                    .or()
                    .like("branch_code", normalizedKeyword)
                    .or()
                    .like("cnaps_code", normalizedKeyword)
                    .or()
                    .like("city", normalizedKeyword)
                    .or()
                    .like("province", normalizedKeyword));
        }
        query.orderByAsc("sort_order").orderByAsc("bank_code").orderByAsc("province").orderByAsc("city").orderByAsc("branch_name");
        query.last("limit 500");
        return systemBankBranchCatalogMapper.selectList(query).stream().map(this::toBranchOption).toList();
    }

    /**
     * 处理财务银行目录中的这一步。
     */
    @Override
    public FinanceBankBranchVO lookupBranchByCnaps(String cnapsCode) {
        String normalizedCnapsCode = trimToNull(cnapsCode);
        if (normalizedCnapsCode == null) {
            return null;
        }
        QueryWrapper<SystemBankBranchCatalog> query = new QueryWrapper<>();
        query.eq("status", 1).eq("cnaps_code", normalizedCnapsCode).last("limit 1");
        SystemBankBranchCatalog branch = systemBankBranchCatalogMapper.selectOne(query);
        return branch == null ? null : toBranchOption(branch);
    }

    private FinanceBankOptionVO toBankOption(SystemBankCatalog bank) {
        FinanceBankOptionVO option = new FinanceBankOptionVO();
        option.setBankCode(bank.getBankCode());
        option.setBankName(bank.getBankName());
        option.setValue(bank.getBankCode());
        option.setLabel(bank.getBankName());
        return option;
    }

    private FinanceBankBranchVO toBranchOption(SystemBankBranchCatalog branch) {
        FinanceBankBranchVO option = new FinanceBankBranchVO();
        option.setId(branch.getId());
        option.setBankCode(branch.getBankCode());
        option.setBankName(branch.getBankName());
        option.setProvince(branch.getProvince());
        option.setCity(branch.getCity());
        option.setBranchCode(branch.getBranchCode());
        option.setBranchName(branch.getBranchName());
        option.setCnapsCode(branch.getCnapsCode());
        option.setValue(branch.getBranchCode());
        option.setLabel(buildBranchLabel(branch));
        return option;
    }

    /**
     * 组装BranchLabel。
     */
    private String buildBranchLabel(SystemBankBranchCatalog branch) {
        String area = List.of(branch.getProvince(), branch.getCity()).stream()
                .map(this::trimToNull)
                .filter(item -> item != null)
                .distinct()
                .reduce((left, right) -> left + " / " + right)
                .orElse("");
        if (!area.isEmpty()) {
            return branch.getBranchName() + " (" + area + ")";
        }
        return branch.getBranchName();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
