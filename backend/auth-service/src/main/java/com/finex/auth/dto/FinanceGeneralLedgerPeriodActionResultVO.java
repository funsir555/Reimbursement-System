package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceGeneralLedgerPeriodActionResultVO {

    private String actionType;

    private String companyId;

    private Integer iyear;

    private Integer iperiod;

    private Integer iyperiod;

    private String periodLabel;

    private String postStatus;

    private String postStatusLabel;

    private String closeStatus;

    private String closeStatusLabel;
}
