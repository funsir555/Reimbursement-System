package com.finex.auth.service.impl.financearchive;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.UserMapper;

import java.util.List;

public class FinanceVendorOptionDomainSupport extends AbstractFinanceVendorArchiveSupport {

    public FinanceVendorOptionDomainSupport(
            FinanceVendorMapper financeVendorMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper
    ) {
        super(financeVendorMapper, userMapper, objectMapper);
    }

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