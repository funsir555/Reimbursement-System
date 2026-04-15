package com.finex.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinanceVendorDetailVO {

    @JsonProperty("cVenCode")
    private String cVenCode;
    @JsonProperty("cVenName")
    private String cVenName;
    @JsonProperty("cVenAbbName")
    private String cVenAbbName;
    @JsonProperty("cVCCode")
    private String cVCCode;
    @JsonProperty("cTrade")
    private String cTrade;
    @JsonProperty("cVenAddress")
    private String cVenAddress;
    @JsonProperty("cVenRegCode")
    private String cVenRegCode;
    @JsonProperty("cVenBank")
    private String cVenBank;
    @JsonProperty("cVenAccount")
    private String cVenAccount;
    @JsonProperty("cVenBankNub")
    private String cVenBankNub;
    @JsonProperty("receiptAccountName")
    private String receiptAccountName;
    @JsonProperty("receiptBankProvince")
    private String receiptBankProvince;
    @JsonProperty("receiptBankCity")
    private String receiptBankCity;
    @JsonProperty("receiptBranchCode")
    private String receiptBranchCode;
    @JsonProperty("receiptBranchName")
    private String receiptBranchName;
    @JsonProperty("cVenPerson")
    private String cVenPerson;
    @JsonProperty("cVenPhone")
    private String cVenPhone;
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
    @JsonProperty("cCreatePerson")
    private String cCreatePerson;
    @JsonProperty("cDCCode")
    private String cDCCode;
    @JsonProperty("cModifyPerson")
    private String cModifyPerson;
    @JsonProperty("cRelCustomer")
    private String cRelCustomer;
    @JsonProperty("cVenBankCode")
    private String cVenBankCode;
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
    @JsonProperty("cVenFax")
    private String cVenFax;
    @JsonProperty("cVenHeadCode")
    private String cVenHeadCode;
    @JsonProperty("cVenIAddress")
    private String cVenIAddress;
    @JsonProperty("cVenIType")
    private String cVenIType;
    @JsonProperty("cVenLPerson")
    private String cVenLPerson;
    @JsonProperty("cVenPayCond")
    private String cVenPayCond;
    @JsonProperty("cVenPostCode")
    private String cVenPostCode;
    @JsonProperty("cVenPPerson")
    private String cVenPPerson;
    @JsonProperty("cVenTradeCCode")
    private String cVenTradeCCode;
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
    @MoneyValue
    @JsonProperty("fRegistFund")
    private BigDecimal fRegistFund;
    @MoneyValue
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
    @MoneyValue
    @JsonProperty("iLastMoney")
    private BigDecimal iLastMoney;
    @JsonProperty("iLicenceADays")
    private Integer iLicenceADays;
    @MoneyValue
    @JsonProperty("iLRMoney")
    private BigDecimal iLRMoney;
    @JsonProperty("iProxyADays")
    private Integer iProxyADays;
    @JsonProperty("iVenCreDate")
    private Integer iVenCreDate;
    @JsonProperty("iVenCreGrade")
    private String iVenCreGrade;
    @MoneyValue
    @JsonProperty("iVenCreLine")
    private BigDecimal iVenCreLine;
    @JsonProperty("iVenDisRate")
    private BigDecimal iVenDisRate;
    @JsonProperty("active")
    private Boolean active;
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}
