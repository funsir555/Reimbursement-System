package com.finex.auth.service;

import com.finex.auth.dto.FinanceCloseLedgerMetaVO;
import com.finex.auth.dto.FinanceCloseLedgerReconcileResultVO;
import com.finex.auth.dto.FinanceCloseLedgerRequestDTO;
import com.finex.auth.dto.FinanceCloseLedgerValidationResultVO;

public interface FinanceCloseLedgerService {

    FinanceCloseLedgerMetaVO getMeta(Long currentUserId, String companyId, Integer iyear, Integer iperiod);

    FinanceCloseLedgerReconcileResultVO reconcile(Long currentUserId, String operatorName, FinanceCloseLedgerRequestDTO dto);

    FinanceCloseLedgerValidationResultVO validate(Long currentUserId, String operatorName, FinanceCloseLedgerRequestDTO dto);

    FinanceCloseLedgerMetaVO close(Long currentUserId, String operatorName, FinanceCloseLedgerRequestDTO dto);
}
