package com.finex.auth.service;

import com.finex.auth.dto.FinancePeriodTransferGenerateDTO;
import com.finex.auth.dto.FinancePeriodTransferGenerateResultVO;
import com.finex.auth.dto.FinancePeriodTransferMetaVO;
import com.finex.auth.dto.FinancePeriodTransferPreviewDTO;
import com.finex.auth.dto.FinancePeriodTransferPreviewResultVO;
import com.finex.auth.dto.FinancePeriodTransferRuleDTO;
import com.finex.auth.dto.FinancePeriodTransferRuleVO;
import com.finex.auth.dto.FinancePeriodTransferRunDetailVO;
import com.finex.auth.dto.FinancePeriodTransferRunVO;
import java.util.List;

public interface FinancePeriodTransferService {

    FinancePeriodTransferMetaVO getMeta(Long currentUserId, String companyId, Integer iyear, Integer iperiod);

    List<FinancePeriodTransferRuleVO> listRules(String companyId);

    FinancePeriodTransferRuleVO saveRule(FinancePeriodTransferRuleDTO dto, Long currentUserId, String currentUsername);

    FinancePeriodTransferPreviewResultVO preview(FinancePeriodTransferPreviewDTO dto, Long currentUserId, String currentUsername);

    FinancePeriodTransferGenerateResultVO generate(FinancePeriodTransferGenerateDTO dto, Long currentUserId, String currentUsername);

    List<FinancePeriodTransferRunVO> listRuns(String companyId, Integer iyear, Integer iperiod, String ruleType);

    FinancePeriodTransferRunDetailVO getRunDetail(String companyId, Long runId);

    boolean hasCompletedRunForPeriod(String companyId, int iyear, int iperiod);

    String resolveValidationMessage(String companyId, int iyear, int iperiod);
}
