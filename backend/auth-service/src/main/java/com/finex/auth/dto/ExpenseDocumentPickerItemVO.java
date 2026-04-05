package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseDocumentPickerItemVO {

    private String documentCode;

    private String documentTitle;

    private String templateType;

    private String templateTypeLabel;

    private String templateName;

    private String status;

    private String statusLabel;

    @MoneyValue
    private BigDecimal totalAmount;

    @MoneyValue
    private BigDecimal availableWriteOffAmount;

    private String writeOffSourceKind;
}
