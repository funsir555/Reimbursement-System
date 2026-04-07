package com.finex.auth.service;

import com.finex.auth.dto.FinanceVoucherDetailVO;
import com.finex.auth.dto.FinanceVoucherMetaVO;
import com.finex.auth.dto.FinanceVoucherPageVO;
import com.finex.auth.dto.FinanceVoucherQueryDTO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;
import com.finex.auth.dto.FinanceVoucherSummaryVO;

public interface FinanceVoucherService {

    FinanceVoucherMetaVO getMeta(Long currentUserId, String currentUsername, String companyId, String billDate, String csign);

    FinanceVoucherPageVO<FinanceVoucherSummaryVO> queryVouchers(FinanceVoucherQueryDTO dto);

    FinanceVoucherDetailVO getDetail(String companyId, String voucherNo);

    FinanceVoucherSaveResultVO saveVoucher(FinanceVoucherSaveDTO dto, Long currentUserId, String currentUsername);

    FinanceVoucherSaveResultVO updateVoucher(String companyId, String voucherNo, FinanceVoucherSaveDTO dto, Long currentUserId, String currentUsername);

    byte[] exportVouchers(FinanceVoucherQueryDTO dto);
}
