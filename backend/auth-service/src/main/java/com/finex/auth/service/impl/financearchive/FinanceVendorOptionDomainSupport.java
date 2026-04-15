// 业务域：财务档案
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service.impl.financearchive;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.UserMapper;

import java.util.List;

/**
 * FinanceVendorOptionDomainSupport：领域规则支撑类。
 * 承接 财务供应商选项的核心业务规则。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
public class FinanceVendorOptionDomainSupport extends AbstractFinanceVendorArchiveSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FinanceVendorOptionDomainSupport(
            FinanceVendorMapper financeVendorMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper
    ) {
        super(financeVendorMapper, userMapper, objectMapper);
    }

    /**
     * 查询Active供应商选项。
     */
    public List<ExpenseCreateVendorOptionVO> listActiveVendorOptions(String companyId, String keyword, Boolean includeDisabled) {
        String normalizedCompanyId = requireCompanyId(companyId);
        QueryWrapper<FinanceVendor> query = new QueryWrapper<>();
        query.eq("company_id", normalizedCompanyId);
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword != null) {
            query.and(wrapper -> wrapper
                    .like("cVenCode", normalizedKeyword)
                    .or()
                    .like("cVenName", normalizedKeyword)
                    .or()
                    .like("cVenAbbName", normalizedKeyword));
        }
        if (!Boolean.TRUE.equals(includeDisabled)) {
            query.isNull("dEndDate");
        }
        query.orderByAsc("dEndDate").orderByAsc("cVenName").orderByAsc("cVenCode");
        return financeVendorMapper.selectList(query).stream()
                .filter(item -> normalizedCompanyId.equals(trimToNull(item.getCompanyId())))
                .map(this::toOption)
                .toList();
    }
}
