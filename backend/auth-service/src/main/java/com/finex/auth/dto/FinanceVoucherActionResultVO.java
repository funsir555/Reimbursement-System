package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceVoucherActionResultVO {

    private String action;

    private String voucherNo;

    private Integer iyear;

    private Integer iyperiod;

    private String status;

    private String statusLabel;

    private String periodStatus;

    private String periodStatusLabel;

    private String checkerName;

    private String checkedAt;

    private String postedAt;

    private String voidedAt;

    private String voidedByName;

    private String reversedFromVoucherNo;

    private String reversedByVoucherNo;

    private String nextVoucherNo;

    private Boolean lastVoucherOfMonth;
}
