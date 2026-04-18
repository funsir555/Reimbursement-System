package com.finex.auth.service.impl.expense;

import java.util.Locale;

public enum ExpenseDocumentPrintOrientation {
    PORTRAIT,
    LANDSCAPE;

    public static ExpenseDocumentPrintOrientation fromRequest(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return PORTRAIT;
        }
        try {
            return ExpenseDocumentPrintOrientation.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return PORTRAIT;
        }
    }

    public String toCssSize() {
        return this == LANDSCAPE ? "landscape" : "portrait";
    }
}
