package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceGeneralLedgerRollbackPeriodResultVO {

    private String companyId;

    private Integer iyear;

    private Integer iperiod;

    private Integer iyperiod;

    private Boolean closedPeriodDetected;

    private Integer rolledBackVoucherCount;

    private Integer rebuildAccsumCount;

    private Integer rebuildAccassCount;

    private String periodStatus;

    private String periodStatusLabel;

    private String postStatus;

    private String postStatusLabel;
}
