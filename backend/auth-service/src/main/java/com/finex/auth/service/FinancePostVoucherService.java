package com.finex.auth.service;

import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.FinancePostVoucherMetaVO;
import com.finex.auth.dto.FinancePostVoucherTaskRequestDTO;
import com.finex.auth.dto.FinancePostVoucherTaskStatusVO;

public interface FinancePostVoucherService {

    FinancePostVoucherMetaVO getMeta(Long currentUserId, String companyId, Integer iyear, Integer iperiod);

    AsyncTaskSubmitResultVO runPosting(Long currentUserId, String operatorName, FinancePostVoucherTaskRequestDTO dto);

    FinancePostVoucherTaskStatusVO getTaskStatus(String taskNo);
}
