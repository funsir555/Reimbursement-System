package com.finex.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FinanceVendorSummaryVO {

    private String cVenCode;
    private String cVenName;
    private String cVenAbbName;
    private String cVCCode;
    private String cVenPerson;
    private String cVenPhone;
    private String cVenBank;
    private String cVenAccount;
    private String companyId;
    private Boolean active;
    private LocalDateTime dEndDate;
    private LocalDateTime updatedAt;
}
