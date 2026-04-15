// 业务域：财务档案
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service.impl.financearchive;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceCustomerDetailVO;
import com.finex.auth.dto.FinanceCustomerSummaryVO;
import com.finex.auth.entity.FinanceCustomer;
import com.finex.auth.mapper.FinanceCustomerMapper;

import java.util.List;

/**
 * FinanceCustomerQueryDomainSupport：领域规则支撑类。
 * 承接 财务客户的核心业务规则。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
public class FinanceCustomerQueryDomainSupport extends AbstractFinanceCustomerArchiveSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FinanceCustomerQueryDomainSupport(
            FinanceCustomerMapper financeCustomerMapper,
            ObjectMapper objectMapper
    ) {
        super(financeCustomerMapper, objectMapper);
    }

    /**
     * 查询客户列表。
     */
    public List<FinanceCustomerSummaryVO> listCustomers(String companyId, String keyword, Boolean includeDisabled) {
        QueryWrapper<FinanceCustomer> query = new QueryWrapper<>();
        query.eq("company_id", requireCompanyId(companyId));
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword != null) {
            query.and(wrapper -> wrapper
                    .like("cCusCode", normalizedKeyword)
                    .or()
                    .like("cCusName", normalizedKeyword)
                    .or()
                    .like("cCusAbbName", normalizedKeyword));
        }
        if (!Boolean.TRUE.equals(includeDisabled)) {
            query.isNull("dEndDate");
        }
        query.orderByAsc("dEndDate").orderByAsc("cCusName").orderByAsc("cCusCode");
        return financeCustomerMapper.selectList(query).stream().map(this::toSummary).toList();
    }

    /**
     * 获取客户明细。
     */
    public FinanceCustomerDetailVO getCustomerDetail(String companyId, String customerCode) {
        return toDetail(requireCustomer(companyId, customerCode));
    }
}
