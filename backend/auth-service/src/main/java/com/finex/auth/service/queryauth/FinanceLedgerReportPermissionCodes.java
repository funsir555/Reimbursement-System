package com.finex.auth.service.queryauth;

import java.util.List;
import java.util.Locale;

/** Existing report view permissions are also the per-role data scope keys. */
public final class FinanceLedgerReportPermissionCodes {
    public static final String BALANCE = "finance:general_ledger:balance_sheet:view";
    public static final String GENERAL = "finance:general_ledger:general_ledger:view";
    public static final String SEQUENCE = "finance:general_ledger:sequence_ledger:view";
    public static final List<String> ALL_VIEWS = List.of(
            BALANCE, GENERAL, SEQUENCE,
            "finance:general_ledger:detail_ledger:view",
            "finance:general_ledger:project_detail_ledger:view",
            "finance:general_ledger:supplier_detail_ledger:view",
            "finance:general_ledger:customer_detail_ledger:view",
            "finance:general_ledger:personal_detail_ledger:view",
            "finance:general_ledger:quantity_amount_detail_ledger:view"
    );

    private FinanceLedgerReportPermissionCodes() { }

    public static String detailView(String ledgerKind) {
        String kind = ledgerKind == null ? "" : ledgerKind.trim().toUpperCase(Locale.ROOT);
        return switch (kind) {
            case "PROJECT" -> "finance:general_ledger:project_detail_ledger:view";
            case "SUPPLIER" -> "finance:general_ledger:supplier_detail_ledger:view";
            case "CUSTOMER" -> "finance:general_ledger:customer_detail_ledger:view";
            case "PERSONAL" -> "finance:general_ledger:personal_detail_ledger:view";
            case "QUANTITY_AMOUNT" -> "finance:general_ledger:quantity_amount_detail_ledger:view";
            default -> "finance:general_ledger:detail_ledger:view";
        };
    }
}
