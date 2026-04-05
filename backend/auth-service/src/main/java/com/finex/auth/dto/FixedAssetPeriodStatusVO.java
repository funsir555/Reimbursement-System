package com.finex.auth.dto;

import lombok.Data;

@Data
public class FixedAssetPeriodStatusVO {
    private String companyId;
    private String bookCode;
    private Integer fiscalYear;
    private Integer fiscalPeriod;
    private String status;
    private String closedBy;
    private String closedAt;
}
