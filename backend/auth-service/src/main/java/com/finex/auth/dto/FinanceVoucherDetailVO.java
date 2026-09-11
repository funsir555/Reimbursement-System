package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceVoucherDetailVO {

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

    private Integer idoc;

    private String cbill;

    private String checkerName;

    private String checkedAt;

    private String postedAt;

    private String ctext1;

    private String ctext2;

    private String status;

    private String statusLabel;

    private Boolean editable;

    private String periodStatus;

    private String periodStatusLabel;

    private String voidedAt;

    private String voidedByName;

    private String reversedFromVoucherNo;

    private String reversedByVoucherNo;

    @MoneyValue
    private BigDecimal totalDebit;

    @MoneyValue
    private BigDecimal totalCredit;

    private List<FinanceVoucherEntryVO> entries = new ArrayList<>();
}
