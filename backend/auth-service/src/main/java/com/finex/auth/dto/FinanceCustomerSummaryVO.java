package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinanceCustomerSummaryVO {

    private String cCusCode;
    private String cCusName;
    private String cCusAbbName;
    private String cCusPerson;
    private String cCusHand;
    private String cCusBank;
    private String cCusAccount;
    @MoneyValue
    private BigDecimal iARMoney;
    private String companyId;
    private Boolean active;
    private LocalDateTime dEndDate;
    private LocalDateTime updatedAt;
}
