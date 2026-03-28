package com.finex.auth.service;

import com.finex.auth.dto.FinanceVoucherDetailVO;
import com.finex.auth.dto.FinanceVoucherMetaVO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;

public interface FinanceVoucherService {

    FinanceVoucherMetaVO getMeta(Long currentUserId, String currentUsername, String companyId, String billDate, String csign);

    FinanceVoucherDetailVO getDetail(String voucherNo);

    FinanceVoucherSaveResultVO saveVoucher(FinanceVoucherSaveDTO dto, Long currentUserId, String currentUsername);
}
