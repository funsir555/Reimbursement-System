package com.finex.auth.service.impl.openingbalance;

import com.finex.auth.dto.OpeningBalanceReconcileResultVO;
import com.finex.auth.dto.OpeningBalanceTrialResultVO;

public class OpeningBalanceTrialReconcileSupport {

    private final SharedOpeningBalanceSupport support;

    public OpeningBalanceTrialReconcileSupport(SharedOpeningBalanceSupport support) {
        this.support = support;
    }

    public OpeningBalanceTrialResultVO trialBalance(String companyId, Integer iyear, Integer iperiod, String operatorName) {
        return support.buildTrialResult(companyId, iyear, iperiod);
    }

    public OpeningBalanceReconcileResultVO reconcile(String companyId, Integer iyear, Integer iperiod, String operatorName) {
        return support.buildReconcileResult(companyId, iyear, iperiod);
    }
}
