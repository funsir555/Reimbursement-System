package com.finex.auth.service.impl.voucher;

import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceCustomerMapper;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;

public final class VoucherMutationDomainSupport extends AbstractFinanceVoucherSupport {

    public VoucherMutationDomainSupport(
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

    public FinanceVoucherSaveResultVO saveVoucher(FinanceVoucherSaveDTO dto, Long currentUserId, String currentUsername) {
        return super.saveVoucher(dto, currentUserId, currentUsername);
    }

    public FinanceVoucherSaveResultVO updateVoucher(
            String companyId,
            String voucherNo,
            FinanceVoucherSaveDTO dto,
            Long currentUserId,
            String currentUsername
    ) {
        return super.updateVoucher(companyId, voucherNo, dto, currentUserId, currentUsername);
    }
}
