package com.finex.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinanceCustomerDetailVO {

    @JsonProperty("cCusCode")
    private String cCusCode;
    @JsonProperty("cCusName")
    private String cCusName;
    @JsonProperty("cCusAbbName")
    private String cCusAbbName;
    @JsonProperty("cCCCode")
    private String cCCCode;
    @JsonProperty("cDCCode")
    private String cDCCode;
    @JsonProperty("cCusTradeCCode")
    private String cCusTradeCCode;
    @JsonProperty("cTrade")
    private String cTrade;
    @JsonProperty("cCusAddress")
    private String cCusAddress;
    @JsonProperty("cCusPostCode")
    private String cCusPostCode;
    @JsonProperty("cCusRegCode")
    private String cCusRegCode;
    @JsonProperty("cCusBank")
    private String cCusBank;
    @JsonProperty("cCusAccount")
    private String cCusAccount;
    @JsonProperty("cCusLPerson")
    private String cCusLPerson;
    @JsonProperty("cCusPerson")
    private String cCusPerson;
    @JsonProperty("cCusHand")
    private String cCusHand;
    @JsonProperty("cCusCreGrade")
    private String cCusCreGrade;
    @MoneyValue
    @JsonProperty("iCusCreLine")
    private BigDecimal iCusCreLine;
    @JsonProperty("iCusCreDate")
    private Integer iCusCreDate;
    @JsonProperty("cCusOAddress")
    private String cCusOAddress;
    @JsonProperty("cCusOType")
    private String cCusOType;
    @JsonProperty("cCusHeadCode")
    private String cCusHeadCode;
    @JsonProperty("cCusWhCode")
    private String cCusWhCode;
    @JsonProperty("cCusDepart")
    private String cCusDepart;
    @MoneyValue
    @JsonProperty("iARMoney")
    private BigDecimal iARMoney;
    @JsonProperty("dLastDate")
    private LocalDateTime dLastDate;
    @MoneyValue
    @JsonProperty("iLastMoney")
    private BigDecimal iLastMoney;
    @JsonProperty("dLRDate")
    private LocalDateTime dLRDate;
    @MoneyValue
    @JsonProperty("iLRMoney")
    private BigDecimal iLRMoney;
    @JsonProperty("dEndDate")
    private LocalDateTime dEndDate;
    @JsonProperty("cCusBankCode")
    private String cCusBankCode;
    @JsonProperty("cCusDefine1")
    private String cCusDefine1;
    @JsonProperty("cCusDefine2")
    private String cCusDefine2;
    @JsonProperty("cCusDefine3")
    private String cCusDefine3;
    @JsonProperty("cCusDefine4")
    private String cCusDefine4;
    @JsonProperty("cCusDefine5")
    private String cCusDefine5;
    @JsonProperty("cCusDefine6")
    private String cCusDefine6;
    @JsonProperty("cCusDefine7")
    private String cCusDefine7;
    @JsonProperty("cCusDefine8")
    private String cCusDefine8;
    @JsonProperty("cCusDefine9")
    private String cCusDefine9;
    @JsonProperty("cCusDefine10")
    private String cCusDefine10;
    @JsonProperty("cCusDefine11")
    private Integer cCusDefine11;
    @JsonProperty("cCusDefine12")
    private Integer cCusDefine12;
    @JsonProperty("cCusDefine13")
    private BigDecimal cCusDefine13;
    @JsonProperty("cCusDefine14")
    private BigDecimal cCusDefine14;
    @JsonProperty("cCusDefine15")
    private LocalDateTime cCusDefine15;
    @JsonProperty("cCusDefine16")
    private LocalDateTime cCusDefine16;
    @JsonProperty("cInvoiceCompany")
    private String cInvoiceCompany;
    @JsonProperty("bCredit")
    private Integer bCredit;
    @JsonProperty("bCreditDate")
    private Integer bCreditDate;
    @JsonProperty("bCreditByHead")
    private Integer bCreditByHead;
    @JsonProperty("cMemo")
    private String cMemo;
    @JsonProperty("fCommisionRate")
    private BigDecimal fCommisionRate;
    @JsonProperty("fInsueRate")
    private BigDecimal fInsueRate;
    @JsonProperty("customerKCode")
    private String customerKCode;
    @JsonProperty("bCusState")
    private Integer bCusState;
    @JsonProperty("companyId")
    private String companyId;
    @JsonProperty("active")
    private Boolean active;
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}
