package com.finex.auth.service.impl.closeledger;

public interface CloseLedgerExternalChecker {

    CloseLedgerExternalCheckResult check(String companyId, int iyear, int iperiod);
}
