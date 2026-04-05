package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseVoucherEntrySnapshotVO {

    private Integer entryNo;
    private String direction;
    private String digest;
    private String accountCode;
    private String accountName;
    private String expenseTypeCode;
    private String expenseTypeName;
    @MoneyValue
    private BigDecimal amount;
}
