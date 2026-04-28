package com.finex.auth.service.impl.closeledger;

public record CloseLedgerExternalCheckResult(
        String code,
        String label,
        boolean passed,
        String message
) {
}
