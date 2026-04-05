package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseVoucherPushResultVO {

    private String documentCode;
    private String companyId;
    private String templateCode;
    private String templateName;
    private String pushStatus;
    private String voucherNo;
    private String errorMessage;
}
