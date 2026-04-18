package com.finex.auth.service;

import com.finex.auth.dto.FinanceCashFlowItemSaveDTO;
import com.finex.auth.dto.FinanceCashFlowItemStatusDTO;
import com.finex.auth.dto.FinanceCashFlowItemSummaryVO;

import java.util.List;

public interface FinanceCashFlowArchiveService {

    List<FinanceCashFlowItemSummaryVO> listCashFlows(String companyId, String keyword, String direction, Integer status);

    FinanceCashFlowItemSummaryVO createCashFlow(String companyId, FinanceCashFlowItemSaveDTO dto);

    FinanceCashFlowItemSummaryVO updateCashFlow(String companyId, Long id, FinanceCashFlowItemSaveDTO dto);

    Boolean updateCashFlowStatus(String companyId, Long id, FinanceCashFlowItemStatusDTO dto);
}
