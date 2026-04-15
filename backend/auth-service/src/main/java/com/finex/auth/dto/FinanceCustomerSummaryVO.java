package com.finex.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinanceCustomerSummaryVO {

    @JsonProperty("cCusCode")
    private String cCusCode;
    @JsonProperty("cCusName")
    private String cCusName;
    @JsonProperty("cCusAbbName")
    private String cCusAbbName;
    @JsonProperty("cCusPerson")
    private String cCusPerson;
    @JsonProperty("cCusHand")
    private String cCusHand;
    @JsonProperty("cCusBank")
    private String cCusBank;
    @JsonProperty("cCusAccount")
    private String cCusAccount;
    @MoneyValue
    @JsonProperty("iARMoney")
    private BigDecimal iARMoney;
    @JsonProperty("companyId")
    private String companyId;
    @JsonProperty("active")
    private Boolean active;
    @JsonProperty("dEndDate")
    private LocalDateTime dEndDate;
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}
