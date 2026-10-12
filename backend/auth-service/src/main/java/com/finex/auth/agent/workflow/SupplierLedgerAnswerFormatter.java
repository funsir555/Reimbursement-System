package com.finex.auth.agent.workflow;

import java.util.List;
import java.util.Map;

/** Renders only the fixed, authorized supplier ledger page returned by Finance. */
final class SupplierLedgerAnswerFormatter {
    private SupplierLedgerAnswerFormatter() {}

    static void appendRows(StringBuilder content, Map<String, Object> item) {
        if (!"finance.query_supplier_detail_ledger".equals(item.get("toolCode"))) return;
        if (!(item.get("records") instanceof List<?> rows)) return;
        for (Object rawRow : rows) {
            if (!(rawRow instanceof Map<?, ?> row)) continue;
            content.append("\n- ").append(safe(row.get("billDate")))
                    .append(" ").append(safe(row.get("voucherNo")))
                    .append(" ").append(safe(row.get("subjectName")))
                    .append(" ").append(safe(row.get("summary")))
                    .append(" 借：").append(safe(row.get("debit")))
                    .append(" 贷：").append(safe(row.get("credit")))
                    .append(" 余额：").append(safe(row.get("balance")));
        }
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value).replace('\n', ' ').replace('\r', ' ');
    }
}
