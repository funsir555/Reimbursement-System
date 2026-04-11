package com.finex.auth.service.impl.voucher;

import com.finex.auth.dto.FinanceVoucherDetailVO;
import com.finex.auth.dto.FinanceVoucherPageVO;
import com.finex.auth.dto.FinanceVoucherQueryDTO;
import com.finex.auth.dto.FinanceVoucherSummaryVO;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceCustomerMapper;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;

public final class VoucherQueryDomainSupport extends AbstractFinanceVoucherSupport {

    public VoucherQueryDomainSupport(
            GlAccvouchMapper glAccvouchMapper,
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            FinanceCustomerMapper financeCustomerMapper,
            FinanceVendorMapper financeVendorMapper,
            FinanceProjectClassMapper financeProjectClassMapper,
            FinanceProjectArchiveMapper financeProjectArchiveMapper,
            SystemCompanyMapper systemCompanyMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper
    ) {
        super(glAccvouchMapper, financeAccountSubjectMapper, financeCustomerMapper, financeVendorMapper, financeProjectClassMapper, financeProjectArchiveMapper, systemCompanyMapper, systemDepartmentMapper, userMapper);
    }

    public FinanceVoucherPageVO<FinanceVoucherSummaryVO> queryVouchers(FinanceVoucherQueryDTO dto) {
        return super.queryVouchers(dto);
    }

    public FinanceVoucherDetailVO getDetail(String companyId, String voucherNo) {
        return super.getDetail(companyId, voucherNo);
    }

    public byte[] exportVouchers(FinanceVoucherQueryDTO dto) {
        return super.exportVouchers(dto);
    }
}
