package com.finex.auth.service;

import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.OpeningBalanceAssistSaveDTO;
import com.finex.auth.dto.OpeningBalanceMetaVO;
import com.finex.auth.dto.OpeningBalanceReconcileResultVO;
import com.finex.auth.dto.OpeningBalanceRowVO;
import com.finex.auth.dto.OpeningBalanceSaveDTO;
import com.finex.auth.dto.OpeningBalanceTaskRequestDTO;
import com.finex.auth.dto.OpeningBalanceTrialResultVO;
import com.finex.auth.dto.OpeningAssistBalanceLineVO;
import java.util.List;

public interface FinanceOpeningBalanceService {

    OpeningBalanceMetaVO getMeta(Long currentUserId, String currentUsername, String companyId, Integer iyear, Integer iperiod);

    List<OpeningBalanceRowVO> listRows(String companyId, Integer iyear, Integer iperiod);

    List<OpeningAssistBalanceLineVO> getAssistBalances(String companyId, Integer iyear, Integer iperiod, String subjectCode);

    List<OpeningBalanceRowVO> saveRows(OpeningBalanceSaveDTO dto, String operatorName);

    List<OpeningAssistBalanceLineVO> saveAssistBalances(String subjectCode, OpeningBalanceAssistSaveDTO dto, String operatorName);

    AsyncTaskSubmitResultVO openBook(Long currentUserId, String operatorName, OpeningBalanceTaskRequestDTO dto);

    AsyncTaskSubmitResultVO carryForward(Long currentUserId, String operatorName, OpeningBalanceTaskRequestDTO dto);

    OpeningBalanceTrialResultVO trialBalance(String companyId, Integer iyear, Integer iperiod, String operatorName);

    OpeningBalanceReconcileResultVO reconcile(String companyId, Integer iyear, Integer iperiod, String operatorName);
}
