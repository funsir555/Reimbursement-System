package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.service.impl.ExpenseDetailSystemFieldSupport;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared amount resolution for expense documents and expense details.
 */
public final class ExpenseAmountResolver {

    public static final String FIELD_DETAIL_AMOUNT = "amount";
    public static final String FIELD_BUSINESS_SCENARIO = ExpenseDetailSystemFieldSupport.FIELD_BUSINESS_SCENARIO;
    public static final String FIELD_INVOICE_AMOUNT = ExpenseDetailSystemFieldSupport.FIELD_INVOICE_AMOUNT;
    public static final String FIELD_ACTUAL_PAYMENT_AMOUNT = ExpenseDetailSystemFieldSupport.FIELD_ACTUAL_PAYMENT_AMOUNT;
    public static final String DETAIL_TYPE_ENTERPRISE = ExpenseDetailSystemFieldSupport.DETAIL_TYPE_ENTERPRISE;
    public static final String MODE_PREPAY_UNBILLED = ExpenseDetailSystemFieldSupport.MODE_PREPAY_UNBILLED;
    public static final String MODE_INVOICE_FULL_PAYMENT = ExpenseDetailSystemFieldSupport.MODE_INVOICE_FULL_PAYMENT;

    private ExpenseAmountResolver() {
    }

    public static BigDecimal resolveDocumentTotalAmount(
            Map<String, Object> formData,
            List<ExpenseDetailInstanceDTO> expenseDetails,
            String defaultBusinessScenario
    ) {
        BigDecimal detailAmount = sumExpenseDetailAmounts(expenseDetails, defaultBusinessScenario);
        return detailAmount != null ? detailAmount : resolveMainFormAmount(formData);
    }

    public static BigDecimal sumExpenseDetailAmounts(
            List<ExpenseDetailInstanceDTO> expenseDetails,
            String defaultBusinessScenario
    ) {
        if (expenseDetails == null || expenseDetails.isEmpty()) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        boolean resolved = false;
        for (ExpenseDetailInstanceDTO detail : expenseDetails) {
            if (detail == null) {
                continue;
            }
            BigDecimal amount = resolveExpenseDetailAmount(
                    detail.getFormData(),
                    detail.getDetailType(),
                    firstNonBlank(detail.getBusinessSceneMode(), detail.getEnterpriseMode(), defaultBusinessScenario)
            );
            if (amount == null) {
                continue;
            }
            total = total.add(amount);
            resolved = true;
        }
        return resolved ? total : null;
    }

    public static BigDecimal resolveExpenseDetailAmount(
            Map<String, Object> detailFormData,
            String detailType,
            String defaultBusinessScenario
    ) {
        Map<String, Object> safeFormData = detailFormData == null ? Collections.emptyMap() : detailFormData;
        BigDecimal actualPaymentAmount = toBigDecimal(safeFormData.get(FIELD_ACTUAL_PAYMENT_AMOUNT));
        if (actualPaymentAmount != null) {
            return actualPaymentAmount;
        }
        String businessScenario = resolveBusinessScenario(
                detailType,
                safeFormData.get(FIELD_BUSINESS_SCENARIO),
                defaultBusinessScenario
        );
        BigDecimal detailAmount = toBigDecimal(safeFormData.get(FIELD_DETAIL_AMOUNT));
        BigDecimal invoiceAmount = toBigDecimal(safeFormData.get(FIELD_INVOICE_AMOUNT));
        if (Objects.equals(detailType, DETAIL_TYPE_ENTERPRISE)) {
            if (Objects.equals(businessScenario, MODE_PREPAY_UNBILLED)) {
                return detailAmount != null ? detailAmount : invoiceAmount;
            }
            return invoiceAmount != null ? invoiceAmount : detailAmount;
        }
        return detailAmount != null ? detailAmount : invoiceAmount;
    }

    public static BigDecimal resolveStoredExpenseDetailAmount(
            Map<String, Object> detailFormData,
            String detailType,
            String businessSceneMode,
            BigDecimal invoiceAmount,
            BigDecimal actualPaymentAmount
    ) {
        Map<String, Object> merged = detailFormData == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(detailFormData);
        if (!merged.containsKey(FIELD_INVOICE_AMOUNT) && invoiceAmount != null) {
            merged.put(FIELD_INVOICE_AMOUNT, invoiceAmount);
        }
        if (!merged.containsKey(FIELD_ACTUAL_PAYMENT_AMOUNT) && actualPaymentAmount != null) {
            merged.put(FIELD_ACTUAL_PAYMENT_AMOUNT, actualPaymentAmount);
        }
        return resolveExpenseDetailAmount(merged, detailType, businessSceneMode);
    }

    public static BigDecimal resolvePrepayWriteOffAmount(
            Map<String, Object> detailFormData,
            BigDecimal actualPaymentAmount
    ) {
        BigDecimal resolvedActualAmount = actualPaymentAmount != null
                ? actualPaymentAmount
                : toBigDecimal(detailFormData == null ? null : detailFormData.get(FIELD_ACTUAL_PAYMENT_AMOUNT));
        return resolvedActualAmount == null ? BigDecimal.ZERO : resolvedActualAmount;
    }

    public static BigDecimal resolveMainFormAmount(Map<String, Object> formData) {
        Map<String, Object> safeFormData = formData == null ? Collections.emptyMap() : formData;
        BigDecimal directAmount = toBigDecimal(safeFormData.get("__totalAmount"));
        if (directAmount != null) {
            return directAmount;
        }
        Object plainAmount = safeFormData.get(FIELD_DETAIL_AMOUNT);
        BigDecimal resolvedAmount = toBigDecimal(plainAmount);
        if (resolvedAmount != null) {
            return resolvedAmount;
        }
        for (Map.Entry<String, Object> entry : safeFormData.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase();
            if (key.contains("amount") || key.contains("money") || key.contains("金額") || key.contains("金额")) {
                BigDecimal amount = toBigDecimal(entry.getValue());
                if (amount != null) {
                    return amount;
                }
            }
        }
        return null;
    }

    public static String resolveBusinessScenario(String detailType, Object rawMode, String defaultBusinessScenario) {
        if (!Objects.equals(detailType, DETAIL_TYPE_ENTERPRISE)) {
            return MODE_INVOICE_FULL_PAYMENT;
        }
        String normalizedMode = trimToNull(rawMode == null ? null : String.valueOf(rawMode));
        if (normalizedMode == null) {
            normalizedMode = trimToNull(defaultBusinessScenario);
        }
        if (!Objects.equals(normalizedMode, MODE_PREPAY_UNBILLED)
                && !Objects.equals(normalizedMode, MODE_INVOICE_FULL_PAYMENT)) {
            return MODE_PREPAY_UNBILLED;
        }
        return normalizedMode;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        String text = trimToNull(String.valueOf(value));
        if (text == null) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
