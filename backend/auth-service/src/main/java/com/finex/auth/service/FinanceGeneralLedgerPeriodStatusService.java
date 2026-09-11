package com.finex.auth.service;

import com.finex.auth.dto.FinanceGeneralLedgerPeriodActionDTO;
import com.finex.auth.dto.FinanceGeneralLedgerPeriodActionResultVO;
import com.finex.auth.dto.FinanceGeneralLedgerPeriodStatusOverviewVO;

public interface FinanceGeneralLedgerPeriodStatusService {

    FinanceGeneralLedgerPeriodStatusOverviewVO getOverview(
            Long currentUserId,
            String companyId,
            Integer iyear,
            Integer currentIyear,
            Integer currentIperiod
    );

    FinanceGeneralLedgerPeriodActionResultVO reopenPeriod(
            Long currentUserId,
            String currentUsername,
            FinanceGeneralLedgerPeriodActionDTO dto
    );

    FinanceGeneralLedgerPeriodActionResultVO unpostPeriod(
            Long currentUserId,
            String currentUsername,
            FinanceGeneralLedgerPeriodActionDTO dto
    );
}
