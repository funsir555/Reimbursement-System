package com.finex.auth.service.impl;

import com.finex.auth.dto.FinanceVoucherDetailVO;
import com.finex.auth.dto.FinanceVoucherMetaVO;
import com.finex.auth.dto.FinanceVoucherPageVO;
import com.finex.auth.dto.FinanceVoucherQueryDTO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;
import com.finex.auth.dto.FinanceVoucherSummaryVO;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceCustomerMapper;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.FinanceVoucherService;
import com.finex.auth.service.UserService;
import com.finex.auth.service.impl.voucher.VoucherContextSupport;
import com.finex.auth.service.impl.voucher.VoucherMetaSupport;
import com.finex.auth.service.impl.voucher.VoucherMutationDomainSupport;
import com.finex.auth.service.impl.voucher.VoucherQueryDomainSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceVoucherServiceImpl implements FinanceVoucherService {

    private final VoucherMetaSupport voucherMetaSupport;
    private final VoucherQueryDomainSupport voucherQueryDomainSupport;
    private final VoucherMutationDomainSupport voucherMutationDomainSupport;

    public FinanceVoucherServiceImpl(
            GlAccvouchMapper glAccvouchMapper,
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            FinanceCustomerMapper financeCustomerMapper,
            FinanceVendorMapper financeVendorMapper,
            FinanceProjectClassMapper financeProjectClassMapper,
            FinanceProjectArchiveMapper financeProjectArchiveMapper,
            SystemCompanyMapper systemCompanyMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper,
            FinanceAccountSetMapper financeAccountSetMapper,
            UserService userService
    ) {
        VoucherContextSupport voucherContextSupport = new VoucherContextSupport(systemCompanyMapper, financeAccountSetMapper, userService);
        this.voucherMetaSupport = new VoucherMetaSupport(
                glAccvouchMapper,
                financeAccountSubjectMapper,
                financeCustomerMapper,
                financeVendorMapper,
                financeProjectClassMapper,
                financeProjectArchiveMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper,
                voucherContextSupport
        );
        this.voucherQueryDomainSupport = new VoucherQueryDomainSupport(
                glAccvouchMapper,
                financeAccountSubjectMapper,
                financeCustomerMapper,
                financeVendorMapper,
                financeProjectClassMapper,
                financeProjectArchiveMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper
        );
        this.voucherMutationDomainSupport = new VoucherMutationDomainSupport(
                glAccvouchMapper,
                financeAccountSubjectMapper,
                financeCustomerMapper,
                financeVendorMapper,
                financeProjectClassMapper,
                financeProjectArchiveMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper
        );
    }

    @Override
    public FinanceVoucherMetaVO getMeta(Long currentUserId, String currentUsername, String companyId, String billDate, String csign) {
        return voucherMetaSupport.getMeta(currentUserId, currentUsername, companyId, billDate, csign);
    }

    @Override
    public FinanceVoucherPageVO<FinanceVoucherSummaryVO> queryVouchers(FinanceVoucherQueryDTO dto) {
        return voucherQueryDomainSupport.queryVouchers(dto);
    }

    @Override
    public FinanceVoucherDetailVO getDetail(String companyId, String voucherNo) {
        return voucherQueryDomainSupport.getDetail(companyId, voucherNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVoucherSaveResultVO saveVoucher(FinanceVoucherSaveDTO dto, Long currentUserId, String currentUsername) {
        return voucherMutationDomainSupport.saveVoucher(dto, currentUserId, currentUsername);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVoucherSaveResultVO updateVoucher(
            String companyId,
            String voucherNo,
            FinanceVoucherSaveDTO dto,
            Long currentUserId,
            String currentUsername
    ) {
        return voucherMutationDomainSupport.updateVoucher(companyId, voucherNo, dto, currentUserId, currentUsername);
    }

    @Override
    public byte[] exportVouchers(FinanceVoucherQueryDTO dto) {
        return voucherQueryDomainSupport.exportVouchers(dto);
    }
}
