package com.finex.auth.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FinanceBalanceSheetRowVO {

    private String subjectCode;

    private String subjectName;

    private Integer subjectLevel;

    private String rowType;

    private String subjectCategory;

    private BigDecimal beginDebit;

    private BigDecimal beginCredit;

    private BigDecimal periodDebit;

    private BigDecimal periodCredit;

    private BigDecimal endDebit;

    private BigDecimal endCredit;
}
