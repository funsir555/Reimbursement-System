package com.finex.auth.service;

import com.finex.auth.dto.FinanceGeneralLedgerRollbackPeriodDTO;
import com.finex.auth.dto.FinanceGeneralLedgerRollbackPeriodResultVO;

public interface FinanceGeneralLedgerMaintenanceService {

    FinanceGeneralLedgerRollbackPeriodResultVO rollbackPeriod(
            Long currentUserId,
            String operatorName,
            FinanceGeneralLedgerRollbackPeriodDTO dto
    );
}
