package com.finex.auth.service.impl.closeledger;

import java.util.List;

public class CloseLedgerExternalCheckerRegistry {

    private final List<CloseLedgerExternalChecker> checkers;

    public CloseLedgerExternalCheckerRegistry(List<CloseLedgerExternalChecker> checkers) {
        this.checkers = checkers == null ? List.of() : List.copyOf(checkers);
    }

    public List<CloseLedgerExternalCheckResult> checkAll(String companyId, int iyear, int iperiod) {
        return checkers.stream()
                .map(checker -> checker.check(companyId, iyear, iperiod))
                .toList();
    }
}
