// 业务域：财务凭证
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 凭证查询、新建、修改等接口，下游会继续协调 凭证主表、分录、上下文数据与报销关联。
// 风险提醒：改坏后最容易影响 凭证金额、分录科目和与单据的对应关系。

package com.finex.auth.service.impl;

import com.finex.auth.dto.FinanceVoucherActionResultVO;
import com.finex.auth.dto.FinanceVoucherBatchActionDTO;
import com.finex.auth.dto.FinanceVoucherBatchActionResultVO;
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

/**
 * FinanceVoucherServiceImpl：service 入口实现。
 * 接住上层请求，并把 财务凭证相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 凭证金额、分录科目和与单据的对应关系是否会被一起带坏。
 */
@Service
public class FinanceVoucherServiceImpl implements FinanceVoucherService {

    private final VoucherMetaSupport voucherMetaSupport;
    private final VoucherQueryDomainSupport voucherQueryDomainSupport;
    private final VoucherMutationDomainSupport voucherMutationDomainSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
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

    /**
     * 获取元数据。
     */
    @Override
    public FinanceVoucherMetaVO getMeta(Long currentUserId, String currentUsername, String companyId, String billDate, String csign) {
        return voucherMetaSupport.getMeta(currentUserId, currentUsername, companyId, billDate, csign);
    }

    /**
     * 查询凭证。
     */
    @Override
    public FinanceVoucherPageVO<FinanceVoucherSummaryVO> queryVouchers(FinanceVoucherQueryDTO dto) {
        return voucherQueryDomainSupport.queryVouchers(dto);
    }

    /**
     * 获取明细。
     */
    @Override
    public FinanceVoucherDetailVO getDetail(String companyId, String voucherNo) {
        return voucherQueryDomainSupport.getDetail(companyId, voucherNo);
    }

    /**
     * 保存凭证。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVoucherSaveResultVO saveVoucher(FinanceVoucherSaveDTO dto, Long currentUserId, String currentUsername) {
        return voucherMutationDomainSupport.saveVoucher(dto, currentUserId, currentUsername);
    }

    /**
     * 更新凭证。
     */
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
    @Transactional(rollbackFor = Exception.class)
    public FinanceVoucherActionResultVO reviewVoucher(String companyId, String voucherNo, Long currentUserId, String currentUsername) {
        return voucherMutationDomainSupport.reviewVoucher(companyId, voucherNo, currentUserId, currentUsername);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVoucherActionResultVO unreviewVoucher(String companyId, String voucherNo) {
        return voucherMutationDomainSupport.unreviewVoucher(companyId, voucherNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVoucherActionResultVO markVoucherError(String companyId, String voucherNo) {
        return voucherMutationDomainSupport.markVoucherError(companyId, voucherNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVoucherActionResultVO clearVoucherError(String companyId, String voucherNo) {
        return voucherMutationDomainSupport.clearVoucherError(companyId, voucherNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVoucherBatchActionResultVO batchUpdateVoucherState(FinanceVoucherBatchActionDTO dto, Long currentUserId, String currentUsername) {
        return voucherMutationDomainSupport.batchUpdateVoucherState(dto, currentUserId, currentUsername);
    }

    /**
     * 处理财务凭证中的这一步。
     */
    @Override
    public byte[] exportVouchers(FinanceVoucherQueryDTO dto) {
        return voucherQueryDomainSupport.exportVouchers(dto);
    }
}
