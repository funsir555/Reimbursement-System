package com.finex.auth.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FinanceDetailLedgerRowVO {

    private String ledgerKind;

    private String groupKey;

    private String rowType;

    private String subjectCode;

    private String subjectName;

    private String assistLabel;

    private String dbillDate;

    private String voucherNo;

    private String displayVoucherNo;

    private String summary;

    private String voucherTypeLabel;

    private String makerName;

    private BigDecimal debit;

    private BigDecimal credit;

    private BigDecimal balance;

    private String balanceDirection;

    private BigDecimal quantityDebit;

    private BigDecimal quantityCredit;

    private BigDecimal quantityBalance;

    private String measureUnit;
}
