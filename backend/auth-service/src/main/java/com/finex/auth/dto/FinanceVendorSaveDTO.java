package com.finex.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.finex.auth.support.json.MoneyInput;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinanceVendorSaveDTO {

    @Size(max = 64, message = "\u4f9b\u5e94\u5546\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenCode")
    private String cVenCode;
    @Size(max = 128, message = "\u4f9b\u5e94\u5546\u540d\u79f0\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 128 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenName")
    private String cVenName;
    @Size(max = 64, message = "\u4f9b\u5e94\u5546\u7b80\u79f0\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenAbbName")
    private String cVenAbbName;
    @Size(max = 64, message = "\u5206\u7c7b\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVCCode")
    private String cVCCode;
    @JsonProperty("cTrade")
    private String cTrade;
    @JsonProperty("cVenAddress")
    private String cVenAddress;
    @JsonProperty("cVenRegCode")
    private String cVenRegCode;
    @Size(max = 128, message = "\u5f00\u6237\u94f6\u884c\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 128 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenBank")
    private String cVenBank;
    @Size(max = 64, message = "\u94f6\u884c\u8d26\u53f7\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenAccount")
    private String cVenAccount;
    @Size(max = 64, message = "\u8054\u884c\u53f7\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenBankNub")
    private String cVenBankNub;
    @Size(max = 128, message = "\u6536\u6b3e\u5f00\u6237\u540d\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 128 \u4e2a\u5b57\u7b26")
    @JsonProperty("receiptAccountName")
    private String receiptAccountName;
    @JsonProperty("receiptBankProvince")
    private String receiptBankProvince;
    @JsonProperty("receiptBankCity")
    private String receiptBankCity;
    @JsonProperty("receiptBranchCode")
    private String receiptBranchCode;
    @Size(max = 128, message = "\u6536\u6b3e\u5206\u652f\u884c\u540d\u79f0\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 128 \u4e2a\u5b57\u7b26")
    @JsonProperty("receiptBranchName")
    private String receiptBranchName;
    @Size(max = 64, message = "\u8054\u7cfb\u4eba\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenPerson")
    private String cVenPerson;
    @Size(max = 32, message = "\u7535\u8bdd\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 32 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenPhone")
    private String cVenPhone;
    @Size(max = 32, message = "\u624b\u673a\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 32 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenHand")
    private String cVenHand;
    @JsonProperty("cVenEmail")
    private String cVenEmail;
    @JsonProperty("companyId")
    private String companyId;
    @JsonProperty("cMemo")
    private String cMemo;
    @JsonProperty("dEndDate")
    private LocalDateTime dEndDate;
    @JsonProperty("bBusinessDate")
    private Integer bBusinessDate;
    @JsonProperty("bLicenceDate")
    private Integer bLicenceDate;
    @JsonProperty("bPassGMP")
    private Integer bPassGMP;
    @JsonProperty("bProxyDate")
    private Integer bProxyDate;
    @JsonProperty("bProxyForeign")
    private Integer bProxyForeign;
    @JsonProperty("bVenCargo")
    private Integer bVenCargo;
    @JsonProperty("bVenService")
    private Integer bVenService;
    @JsonProperty("bVenTax")
    private Integer bVenTax;
    @JsonProperty("cBarCode")
    private String cBarCode;
    @Size(max = 64, message = "\u5efa\u6863\u4eba\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cCreatePerson")
    private String cCreatePerson;
    @Size(max = 64, message = "\u5730\u533a\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cDCCode")
    private String cDCCode;
    @Size(max = 64, message = "\u53d8\u66f4\u4eba\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cModifyPerson")
    private String cModifyPerson;
    @Size(max = 64, message = "\u5173\u8054\u5ba2\u6237\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cRelCustomer")
    private String cRelCustomer;
    @Size(max = 64, message = "\u5f00\u6237\u94f6\u884c\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenBankCode")
    private String cVenBankCode;
    @Size(max = 32, message = "\u547c\u673a\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 32 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenBP")
    private String cVenBP;
    @JsonProperty("cVenDefine10")
    private String cVenDefine10;
    @JsonProperty("cVenDefine11")
    private Integer cVenDefine11;
    @JsonProperty("cVenDefine12")
    private Integer cVenDefine12;
    @JsonProperty("cVenDefine13")
    private BigDecimal cVenDefine13;
    @JsonProperty("cVenDefine14")
    private BigDecimal cVenDefine14;
    @JsonProperty("cVenDefine15")
    private LocalDateTime cVenDefine15;
    @JsonProperty("cVenDefine16")
    private LocalDateTime cVenDefine16;
    @JsonProperty("cVenDefine3")
    private String cVenDefine3;
    @JsonProperty("cVenDefine4")
    private String cVenDefine4;
    @JsonProperty("cVenDefine5")
    private String cVenDefine5;
    @JsonProperty("cVenDefine6")
    private String cVenDefine6;
    @JsonProperty("cVenDefine7")
    private String cVenDefine7;
    @JsonProperty("cVenDefine8")
    private String cVenDefine8;
    @JsonProperty("cVenDefine9")
    private String cVenDefine9;
    @JsonProperty("cVenDepart")
    private String cVenDepart;
    @Size(max = 32, message = "\u4f20\u771f\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 32 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenFax")
    private String cVenFax;
    @Size(max = 64, message = "\u4e0a\u7ea7\u4f9b\u5e94\u5546\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenHeadCode")
    private String cVenHeadCode;
    @JsonProperty("cVenIAddress")
    private String cVenIAddress;
    @JsonProperty("cVenIType")
    private String cVenIType;
    @Size(max = 64, message = "\u6cd5\u4eba\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenLPerson")
    private String cVenLPerson;
    @Size(max = 64, message = "\u4ed8\u6b3e\u6761\u4ef6\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenPayCond")
    private String cVenPayCond;
    @Size(max = 16, message = "\u90ae\u653f\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 16 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenPostCode")
    private String cVenPostCode;
    @Size(max = 64, message = "\u4e13\u8425\u4e1a\u52a1\u5458\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenPPerson")
    private String cVenPPerson;
    @Size(max = 64, message = "\u884c\u4e1a\u5206\u7c7b\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenTradeCCode")
    private String cVenTradeCCode;
    @Size(max = 64, message = "\u4ed3\u5e93\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    @JsonProperty("cVenWhCode")
    private String cVenWhCode;
    @JsonProperty("dBusinessEDate")
    private LocalDateTime dBusinessEDate;
    @JsonProperty("dBusinessSDate")
    private LocalDateTime dBusinessSDate;
    @JsonProperty("dLastDate")
    private LocalDateTime dLastDate;
    @JsonProperty("dLicenceEDate")
    private LocalDateTime dLicenceEDate;
    @JsonProperty("dLicenceSDate")
    private LocalDateTime dLicenceSDate;
    @JsonProperty("dLRDate")
    private LocalDateTime dLRDate;
    @JsonProperty("dModifyDate")
    private LocalDateTime dModifyDate;
    @JsonProperty("dProxyEDate")
    private LocalDateTime dProxyEDate;
    @JsonProperty("dProxySDate")
    private LocalDateTime dProxySDate;
    @JsonProperty("dVenDevDate")
    private LocalDateTime dVenDevDate;
    @MoneyInput
    @JsonProperty("fRegistFund")
    private BigDecimal fRegistFund;
    @MoneyInput
    @JsonProperty("iAPMoney")
    private BigDecimal iAPMoney;
    @JsonProperty("iBusinessADays")
    private Integer iBusinessADays;
    @JsonProperty("iEmployeeNum")
    private Integer iEmployeeNum;
    @JsonProperty("iFrequency")
    private Integer iFrequency;
    @JsonProperty("iGradeABC")
    private Integer iGradeABC;
    @JsonProperty("iId")
    private Integer iId;
    @MoneyInput
    @JsonProperty("iLastMoney")
    private BigDecimal iLastMoney;
    @JsonProperty("iLicenceADays")
    private Integer iLicenceADays;
    @MoneyInput
    @JsonProperty("iLRMoney")
    private BigDecimal iLRMoney;
    @JsonProperty("iProxyADays")
    private Integer iProxyADays;
    @JsonProperty("iVenCreDate")
    private Integer iVenCreDate;
    @JsonProperty("iVenCreGrade")
    private String iVenCreGrade;
    @MoneyInput
    @JsonProperty("iVenCreLine")
    private BigDecimal iVenCreLine;
    @JsonProperty("iVenDisRate")
    private BigDecimal iVenDisRate;
}
