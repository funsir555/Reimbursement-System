package com.finex.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FinanceVendorSummaryVO {

    @JsonProperty("cVenCode")
    private String cVenCode;
    @JsonProperty("cVenName")
    private String cVenName;
    @JsonProperty("cVenAbbName")
    private String cVenAbbName;
    @JsonProperty("cVCCode")
    private String cVCCode;
    @JsonProperty("cVenPerson")
    private String cVenPerson;
    @JsonProperty("cVenPhone")
    private String cVenPhone;
    @JsonProperty("cVenBank")
    private String cVenBank;
    @JsonProperty("cVenAccount")
    private String cVenAccount;
    @JsonProperty("companyId")
    private String companyId;
    @JsonProperty("active")
    private Boolean active;
    @JsonProperty("dEndDate")
    private LocalDateTime dEndDate;
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}
