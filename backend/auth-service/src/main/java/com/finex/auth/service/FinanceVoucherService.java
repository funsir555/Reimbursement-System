// 业务域：财务凭证
// 文件角色：service 接口
// 上下游关系：上游通常来自 凭证查询、新建、修改等接口，下游会继续协调 凭证主表、分录、上下文数据与报销关联。
// 风险提醒：改坏后最容易影响 凭证金额、分录科目和与单据的对应关系。

package com.finex.auth.service;

import com.finex.auth.dto.FinanceVoucherDetailVO;
import com.finex.auth.dto.FinanceVoucherMetaVO;
import com.finex.auth.dto.FinanceVoucherPageVO;
import com.finex.auth.dto.FinanceVoucherQueryDTO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;
import com.finex.auth.dto.FinanceVoucherSummaryVO;

/**
 * FinanceVoucherService：service 接口。
 * 定义财务凭证这块对外提供的业务入口能力。
 * 改这里时，要特别关注 凭证金额、分录科目和与单据的对应关系是否会被一起带坏。
 */
public interface FinanceVoucherService {

    /**
     * 获取元数据。
     */
    FinanceVoucherMetaVO getMeta(Long currentUserId, String currentUsername, String companyId, String billDate, String csign);

    /**
     * 查询凭证。
     */
    FinanceVoucherPageVO<FinanceVoucherSummaryVO> queryVouchers(FinanceVoucherQueryDTO dto);

    /**
     * 获取明细。
     */
    FinanceVoucherDetailVO getDetail(String companyId, String voucherNo);

    /**
     * 保存凭证。
     */
    FinanceVoucherSaveResultVO saveVoucher(FinanceVoucherSaveDTO dto, Long currentUserId, String currentUsername);

    /**
     * 更新凭证。
     */
    FinanceVoucherSaveResultVO updateVoucher(String companyId, String voucherNo, FinanceVoucherSaveDTO dto, Long currentUserId, String currentUsername);

    byte[] exportVouchers(FinanceVoucherQueryDTO dto);
}
