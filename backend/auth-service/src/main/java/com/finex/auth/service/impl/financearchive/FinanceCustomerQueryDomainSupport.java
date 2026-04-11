package com.finex.auth.service.impl.financearchive;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceCustomerDetailVO;
import com.finex.auth.dto.FinanceCustomerSummaryVO;
import com.finex.auth.entity.FinanceCustomer;
import com.finex.auth.mapper.FinanceCustomerMapper;

import java.util.List;

public class FinanceCustomerQueryDomainSupport extends AbstractFinanceCustomerArchiveSupport {

    public FinanceCustomerQueryDomainSupport(
            FinanceCustomerMapper financeCustomerMapper,
            ObjectMapper objectMapper
    ) {
        super(financeCustomerMapper, objectMapper);
    }

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

    public FinanceCustomerDetailVO getCustomerDetail(String companyId, String customerCode) {
        return toDetail(requireCustomer(companyId, customerCode));
    }
}
