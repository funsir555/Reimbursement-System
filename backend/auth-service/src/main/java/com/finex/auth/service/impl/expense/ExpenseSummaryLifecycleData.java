package com.finex.auth.service.impl.expense;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

record ExpenseSummaryLifecycleData(
        Map<String, LocalDateTime> latestSubmitAtByDocumentCode,
        Set<String> formalProcessHistoryDocumentCodes
) {
    static ExpenseSummaryLifecycleData empty() {
        return new ExpenseSummaryLifecycleData(Collections.emptyMap(), Collections.emptySet());
    }
}
