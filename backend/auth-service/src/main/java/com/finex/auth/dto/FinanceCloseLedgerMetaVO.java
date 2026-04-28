package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceCloseLedgerMetaVO {

    private String companyId;

    private String companyName;

    private Integer iyear;

    private Integer iperiod;

    private Integer iyperiod;

    private String periodLabel;

    private String status;

    private String statusLabel;

    private String closeNote;

    private String closedBy;

    private String closedAt;

    private String postStatus;

    private String postStatusLabel;

    private Integer unpostedVoucherCount;

    private Integer reviewedVoucherCount;

    private Integer errorVoucherCount;

    private Integer postedVoucherCount;

    private Boolean fixedAssetClosed;

    private String fixedAssetStatusLabel;
}
