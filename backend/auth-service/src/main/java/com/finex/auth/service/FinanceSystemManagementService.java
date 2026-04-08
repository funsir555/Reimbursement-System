package com.finex.auth.service;

import com.finex.auth.dto.FinanceAccountSetCreateDTO;
import com.finex.auth.dto.FinanceAccountSetMetaVO;
import com.finex.auth.dto.FinanceAccountSetSummaryVO;
import com.finex.auth.dto.FinanceAccountSetTaskStatusVO;

import java.util.List;

public interface FinanceSystemManagementService {

    FinanceAccountSetMetaVO getMeta();

    List<FinanceAccountSetSummaryVO> listAccountSets();

    FinanceAccountSetTaskStatusVO submitCreateTask(Long currentUserId, FinanceAccountSetCreateDTO dto);

    FinanceAccountSetTaskStatusVO getTaskStatus(String taskNo);
}
