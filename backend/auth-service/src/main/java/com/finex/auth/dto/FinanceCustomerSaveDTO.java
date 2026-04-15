package com.finex.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.finex.auth.support.json.MoneyInput;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinanceCustomerSaveDTO {

    @Size(max = 64, message = "\u5ba2\u6237\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cCusCode")
    private String cCusCode;
    @Size(max = 128, message = "\u5ba2\u6237\u540d\u79f0\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 128 \u4e2a\u5b57\u7b26")
    @JsonProperty("cCusName")
    private String cCusName;
    @Size(max = 64, message = "\u5ba2\u6237\u7b80\u79f0\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cCusAbbName")
    private String cCusAbbName;
    @Size(max = 64, message = "\u5206\u7c7b\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cCCCode")
    private String cCCCode;
    @Size(max = 64, message = "\u5730\u533a\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cDCCode")
    private String cDCCode;
    @Size(max = 64, message = "\u884c\u4e1a\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cCusTradeCCode")
    private String cCusTradeCCode;
    @JsonProperty("cTrade")
    private String cTrade;
    @JsonProperty("cCusAddress")
    private String cCusAddress;
    @Size(max = 16, message = "\u90ae\u653f\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 16 \u4e2a\u5b57\u7b26")
    @JsonProperty("cCusPostCode")
    private String cCusPostCode;
    @JsonProperty("cCusRegCode")
    private String cCusRegCode;
    @Size(max = 128, message = "\u5f00\u6237\u94f6\u884c\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 128 \u4e2a\u5b57\u7b26")
    @JsonProperty("cCusBank")
    private String cCusBank;
    @Size(max = 64, message = "\u94f6\u884c\u8d26\u53f7\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cCusAccount")
    private String cCusAccount;
    @Size(max = 64, message = "\u6cd5\u4eba\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cCusLPerson")
    private String cCusLPerson;
    @Size(max = 64, message = "\u8054\u7cfb\u4eba\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cCusPerson")
    private String cCusPerson;
    @Size(max = 32, message = "\u624b\u673a\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 32 \u4e2a\u5b57\u7b26")
    @JsonProperty("cCusHand")
    private String cCusHand;
    @JsonProperty("cCusCreGrade")
    private String cCusCreGrade;
    @MoneyInput
    @JsonProperty("iCusCreLine")
    private BigDecimal iCusCreLine;
    @JsonProperty("iCusCreDate")
    private Integer iCusCreDate;
    @JsonProperty("cCusOAddress")
    private String cCusOAddress;
    @JsonProperty("cCusOType")
    private String cCusOType;
    @Size(max = 64, message = "\u5ba2\u6237\u603b\u516c\u53f8\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cCusHeadCode")
    private String cCusHeadCode;
    @Size(max = 64, message = "\u53d1\u8d27\u4ed3\u5e93\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cCusWhCode")
    private String cCusWhCode;
    @Size(max = 64, message = "\u5206\u7ba1\u90e8\u95e8\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cCusDepart")
    private String cCusDepart;
    @MoneyInput
    @JsonProperty("iARMoney")
    private BigDecimal iARMoney;
    @JsonProperty("dLastDate")
    private LocalDateTime dLastDate;
    @MoneyInput
    @JsonProperty("iLastMoney")
    private BigDecimal iLastMoney;
    @JsonProperty("dLRDate")
    private LocalDateTime dLRDate;
    @MoneyInput
    @JsonProperty("iLRMoney")
    private BigDecimal iLRMoney;
    @JsonProperty("dEndDate")
    private LocalDateTime dEndDate;
    @Size(max = 64, message = "\u6240\u5c5e\u94f6\u884c\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
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
    @Size(max = 64, message = "\u5ba2\u6237\u7ea7\u522b\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("customerKCode")
    private String customerKCode;
    @JsonProperty("bCusState")
    private Integer bCusState;
    @JsonProperty("companyId")
    private String companyId;
}
