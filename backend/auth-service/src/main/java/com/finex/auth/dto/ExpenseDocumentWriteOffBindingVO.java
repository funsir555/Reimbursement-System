package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseDocumentWriteOffBindingVO {

    private String direction;

    private String fieldKey;

    private String documentCode;

    private String documentTitle;

    private String templateType;

    private String templateTypeLabel;

    private String templateName;

    private String status;

    private String statusLabel;

    private String submitterName;

    private String writeOffSourceKind;

    @MoneyValue
    private BigDecimal requestedAmount;

    @MoneyValue
    private BigDecimal effectiveAmount;

    @MoneyValue
    private BigDecimal remainingAmount;

    private String effectiveStatus;

    private String effectiveStatusLabel;
}
