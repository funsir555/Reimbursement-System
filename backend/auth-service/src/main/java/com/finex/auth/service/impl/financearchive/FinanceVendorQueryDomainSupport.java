package com.finex.auth.service.impl.financearchive;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSummaryVO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.UserMapper;

import java.util.List;

public class FinanceVendorQueryDomainSupport extends AbstractFinanceVendorArchiveSupport {

    public FinanceVendorQueryDomainSupport(
            FinanceVendorMapper financeVendorMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper
    ) {
        super(financeVendorMapper, userMapper, objectMapper);
    }

    public List<FinanceVendorSummaryVO> listVendors(String companyId, String keyword, Boolean includeDisabled) {
        QueryWrapper<FinanceVendor> query = new QueryWrapper<>();
        query.eq("company_id", requireCompanyId(companyId));
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
        return financeVendorMapper.selectList(query).stream().map(this::toSummary).toList();
    }

    public FinanceVendorDetailVO getVendorDetail(String companyId, String vendorCode) {
        return toDetail(requireVendor(companyId, vendorCode));
    }
}