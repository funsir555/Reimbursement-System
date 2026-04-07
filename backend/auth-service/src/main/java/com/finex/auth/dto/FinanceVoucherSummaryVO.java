package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FinanceVoucherSummaryVO {

    private String voucherNo;

    private String displayVoucherNo;

    private String companyId;

    private Integer iperiod;

    private String csign;

    private String voucherTypeLabel;

    private Integer inoId;

    private String dbillDate;

    private String summary;

    private String cbill;

    private Integer idoc;

    private String status;

    private String statusLabel;

    private Boolean editable;

    private Integer entryCount;

    @MoneyValue
    private BigDecimal totalDebit;

    @MoneyValue
    private BigDecimal totalCredit;
}
