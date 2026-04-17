package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceVoucherActionResultVO {

    private String action;

    private String voucherNo;

    private String status;

    private String statusLabel;

    private String checkerName;

    private String nextVoucherNo;

    private Boolean lastVoucherOfMonth;
}
