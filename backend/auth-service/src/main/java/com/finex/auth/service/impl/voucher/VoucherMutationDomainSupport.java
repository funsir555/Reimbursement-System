// 业务域：财务凭证
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 凭证查询、新建、修改等接口，下游会继续协调 凭证主表、分录、上下文数据与报销关联。
// 风险提醒：改坏后最容易影响 凭证金额、分录科目和与单据的对应关系。

package com.finex.auth.service.impl.voucher;

import com.finex.auth.dto.FinanceVoucherActionResultVO;
import com.finex.auth.dto.FinanceVoucherBatchActionDTO;
import com.finex.auth.dto.FinanceVoucherBatchActionResultVO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceCashFlowItemMapper;
import com.finex.auth.mapper.FinanceCustomerMapper;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;

/**
 * VoucherMutationDomainSupport：领域规则支撑类。
 * 承接 凭证的核心业务规则。
 * 改这里时，要特别关注 凭证金额、分录科目和与单据的对应关系是否会被一起带坏。
 */
public final class VoucherMutationDomainSupport extends AbstractFinanceVoucherSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public VoucherMutationDomainSupport(
            GlAccvouchMapper glAccvouchMapper,
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            FinanceCashFlowItemMapper financeCashFlowItemMapper,
            FinanceCustomerMapper financeCustomerMapper,
            FinanceVendorMapper financeVendorMapper,
            FinanceProjectClassMapper financeProjectClassMapper,
            FinanceProjectArchiveMapper financeProjectArchiveMapper,
            SystemCompanyMapper systemCompanyMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper
    ) {
        super(glAccvouchMapper, financeAccountSubjectMapper, financeCashFlowItemMapper, financeCustomerMapper, financeVendorMapper, financeProjectClassMapper, financeProjectArchiveMapper, systemCompanyMapper, systemDepartmentMapper, userMapper);
    }

    /**
     * 保存凭证。
     */
    public FinanceVoucherSaveResultVO saveVoucher(FinanceVoucherSaveDTO dto, Long currentUserId, String currentUsername) {
        return super.saveVoucher(dto, currentUserId, currentUsername);
    }

    /**
     * 更新凭证。
     */
    public FinanceVoucherSaveResultVO updateVoucher(
            String companyId,
            String voucherNo,
            FinanceVoucherSaveDTO dto,
            Long currentUserId,
            String currentUsername
    ) {
        return super.updateVoucher(companyId, voucherNo, dto, currentUserId, currentUsername);
    }

    public FinanceVoucherActionResultVO reviewVoucher(String companyId, String voucherNo, Long currentUserId, String currentUsername) {
        return super.reviewVoucher(companyId, voucherNo, currentUserId, currentUsername);
    }

    public FinanceVoucherActionResultVO unreviewVoucher(String companyId, String voucherNo) {
        return super.unreviewVoucher(companyId, voucherNo);
    }

    public FinanceVoucherActionResultVO markVoucherError(String companyId, String voucherNo) {
        return super.markVoucherError(companyId, voucherNo);
    }

    public FinanceVoucherActionResultVO clearVoucherError(String companyId, String voucherNo) {
        return super.clearVoucherError(companyId, voucherNo);
    }

    public FinanceVoucherBatchActionResultVO batchUpdateVoucherState(FinanceVoucherBatchActionDTO dto, Long currentUserId, String currentUsername) {
        return super.batchUpdateVoucherState(dto, currentUserId, currentUsername);
    }
}
