package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FinanceVoucherSummaryVO {

    private String voucherNo;

    private String displayVoucherNo;

    private String companyId;

    private Integer iyear;

    private Integer iyperiod;

    private Integer iperiod;

    private String csign;

    private String voucherTypeLabel;

    private Integer inoId;

    private String dbillDate;

    private String summary;

    private String cbill;

    private String checkerName;

    private String checkedAt;

    private String postedAt;

    private Integer idoc;

    private String status;

    private String statusLabel;

    private Boolean editable;

    private String periodStatus;

    private String periodStatusLabel;

    private String voidedAt;

    private String voidedByName;

    private String reversedFromVoucherNo;

    private String reversedByVoucherNo;

    private Integer entryCount;

    @MoneyValue
    private BigDecimal totalDebit;

    @MoneyValue
    private BigDecimal totalCredit;
}
