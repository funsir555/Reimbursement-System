package com.finex.auth.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("gl_Vender")
public class FinanceVendor {

    @TableId(value = "cVenCode", type = IdType.INPUT)
    @JsonProperty("cVenCode")
    private String cVenCode;

    @TableField("cVenName")
    @JsonProperty("cVenName")
    private String cVenName;
    @TableField("cVenAbbName")
    @JsonProperty("cVenAbbName")
    private String cVenAbbName;
    @TableField("cVCCode")
    @JsonProperty("cVCCode")
    private String cVCCode;
    @TableField("cTrade")
    @JsonProperty("cTrade")
    private String cTrade;
    @TableField("cVenAddress")
    @JsonProperty("cVenAddress")
    private String cVenAddress;
    @TableField("cVenRegCode")
    @JsonProperty("cVenRegCode")
    private String cVenRegCode;
    @TableField("cVenBank")
    @JsonProperty("cVenBank")
    private String cVenBank;
    @TableField("cVenAccount")
    @JsonProperty("cVenAccount")
    private String cVenAccount;
    @TableField("cVenBankNub")
    @JsonProperty("cVenBankNub")
    private String cVenBankNub;
    @TableField("receipt_account_name")
    @JsonProperty("receiptAccountName")
    private String receiptAccountName;
    @TableField("receipt_bank_province")
    @JsonProperty("receiptBankProvince")
    private String receiptBankProvince;
    @TableField("receipt_bank_city")
    @JsonProperty("receiptBankCity")
    private String receiptBankCity;
    @TableField("receipt_branch_code")
    @JsonProperty("receiptBranchCode")
    private String receiptBranchCode;
    @TableField("receipt_branch_name")
    @JsonProperty("receiptBranchName")
    private String receiptBranchName;
    @TableField("cVenPerson")
    @JsonProperty("cVenPerson")
    private String cVenPerson;
    @TableField("cVenPhone")
    @JsonProperty("cVenPhone")
    private String cVenPhone;
    @TableField("cVenHand")
    @JsonProperty("cVenHand")
    private String cVenHand;
    @TableField("cVenEmail")
    @JsonProperty("cVenEmail")
    private String cVenEmail;

    @TableField("company_id")
    @JsonProperty("companyId")
    private String companyId;

    @TableField("cMemo")
    @JsonProperty("cMemo")
    private String cMemo;
    @TableField("dEndDate")
    @JsonProperty("dEndDate")
    private LocalDateTime dEndDate;
    @TableField("bBusinessDate")
    @JsonProperty("bBusinessDate")
    private Integer bBusinessDate;
    @TableField("bLicenceDate")
    @JsonProperty("bLicenceDate")
    private Integer bLicenceDate;
    @TableField("bPassGMP")
    @JsonProperty("bPassGMP")
    private Integer bPassGMP;
    @TableField("bProxyDate")
    @JsonProperty("bProxyDate")
    private Integer bProxyDate;
    @TableField("bProxyForeign")
    @JsonProperty("bProxyForeign")
    private Integer bProxyForeign;
    @TableField("bVenCargo")
    @JsonProperty("bVenCargo")
    private Integer bVenCargo;
    @TableField("bVenService")
    @JsonProperty("bVenService")
    private Integer bVenService;
    @TableField("bVenTax")
    @JsonProperty("bVenTax")
    private Integer bVenTax;
    @TableField("cBarCode")
    @JsonProperty("cBarCode")
    private String cBarCode;
    @TableField("cCreatePerson")
    @JsonProperty("cCreatePerson")
    private String cCreatePerson;
    @TableField("cDCCode")
    @JsonProperty("cDCCode")
    private String cDCCode;
    @TableField("cModifyPerson")
    @JsonProperty("cModifyPerson")
    private String cModifyPerson;
    @TableField("cRelCustomer")
    @JsonProperty("cRelCustomer")
    private String cRelCustomer;
    @TableField("cVenBankCode")
    @JsonProperty("cVenBankCode")
    private String cVenBankCode;
    @TableField("cVenBP")
    @JsonProperty("cVenBP")
    private String cVenBP;
    @TableField("cVenDefine10")
    @JsonProperty("cVenDefine10")
    private String cVenDefine10;
    @TableField("cVenDefine11")
    @JsonProperty("cVenDefine11")
    private Integer cVenDefine11;
    @TableField("cVenDefine12")
    @JsonProperty("cVenDefine12")
    private Integer cVenDefine12;
    @TableField("cVenDefine13")
    @JsonProperty("cVenDefine13")
    private BigDecimal cVenDefine13;
    @TableField("cVenDefine14")
    @JsonProperty("cVenDefine14")
    private BigDecimal cVenDefine14;
    @TableField("cVenDefine15")
    @JsonProperty("cVenDefine15")
    private LocalDateTime cVenDefine15;
    @TableField("cVenDefine16")
    @JsonProperty("cVenDefine16")
    private LocalDateTime cVenDefine16;
    @TableField("cVenDefine3")
    @JsonProperty("cVenDefine3")
    private String cVenDefine3;
    @TableField("cVenDefine4")
    @JsonProperty("cVenDefine4")
    private String cVenDefine4;
    @TableField("cVenDefine5")
    @JsonProperty("cVenDefine5")
    private String cVenDefine5;
    @TableField("cVenDefine6")
    @JsonProperty("cVenDefine6")
    private String cVenDefine6;
    @TableField("cVenDefine7")
    @JsonProperty("cVenDefine7")
    private String cVenDefine7;
    @TableField("cVenDefine8")
    @JsonProperty("cVenDefine8")
    private String cVenDefine8;
    @TableField("cVenDefine9")
    @JsonProperty("cVenDefine9")
    private String cVenDefine9;
    @TableField("cVenDepart")
    @JsonProperty("cVenDepart")
    private String cVenDepart;
    @TableField("cVenFax")
    @JsonProperty("cVenFax")
    private String cVenFax;
    @TableField("cVenHeadCode")
    @JsonProperty("cVenHeadCode")
    private String cVenHeadCode;
    @TableField("cVenIAddress")
    @JsonProperty("cVenIAddress")
    private String cVenIAddress;
    @TableField("cVenIType")
    @JsonProperty("cVenIType")
    private String cVenIType;
    @TableField("cVenLPerson")
    @JsonProperty("cVenLPerson")
    private String cVenLPerson;
    @TableField("cVenPayCond")
    @JsonProperty("cVenPayCond")
    private String cVenPayCond;
    @TableField("cVenPostCode")
    @JsonProperty("cVenPostCode")
    private String cVenPostCode;
    @TableField("cVenPPerson")
    @JsonProperty("cVenPPerson")
    private String cVenPPerson;
    @TableField("cVenTradeCCode")
    @JsonProperty("cVenTradeCCode")
    private String cVenTradeCCode;
    @TableField("cVenWhCode")
    @JsonProperty("cVenWhCode")
    private String cVenWhCode;
    @TableField("dBusinessEDate")
    @JsonProperty("dBusinessEDate")
    private LocalDateTime dBusinessEDate;
    @TableField("dBusinessSDate")
    @JsonProperty("dBusinessSDate")
    private LocalDateTime dBusinessSDate;
    @TableField("dLastDate")
    @JsonProperty("dLastDate")
    private LocalDateTime dLastDate;
    @TableField("dLicenceEDate")
    @JsonProperty("dLicenceEDate")
    private LocalDateTime dLicenceEDate;
    @TableField("dLicenceSDate")
    @JsonProperty("dLicenceSDate")
    private LocalDateTime dLicenceSDate;
    @TableField("dLRDate")
    @JsonProperty("dLRDate")
    private LocalDateTime dLRDate;
    @TableField("dModifyDate")
    @JsonProperty("dModifyDate")
    private LocalDateTime dModifyDate;
    @TableField("dProxyEDate")
    @JsonProperty("dProxyEDate")
    private LocalDateTime dProxyEDate;
    @TableField("dProxySDate")
    @JsonProperty("dProxySDate")
    private LocalDateTime dProxySDate;
    @TableField("dVenDevDate")
    @JsonProperty("dVenDevDate")
    private LocalDateTime dVenDevDate;
    @TableField("fRegistFund")
    @JsonProperty("fRegistFund")
    private BigDecimal fRegistFund;
    @TableField("iAPMoney")
    @JsonProperty("iAPMoney")
    private BigDecimal iAPMoney;
    @TableField("iBusinessADays")
    @JsonProperty("iBusinessADays")
    private Integer iBusinessADays;
    @TableField("iEmployeeNum")
    @JsonProperty("iEmployeeNum")
    private Integer iEmployeeNum;
    @TableField("iFrequency")
    @JsonProperty("iFrequency")
    private Integer iFrequency;
    @TableField("iGradeABC")
    @JsonProperty("iGradeABC")
    private Integer iGradeABC;
    @TableField("iId")
    @JsonProperty("iId")
    private Integer iId;
    @TableField("iLastMoney")
    @JsonProperty("iLastMoney")
    private BigDecimal iLastMoney;
    @TableField("iLicenceADays")
    @JsonProperty("iLicenceADays")
    private Integer iLicenceADays;
    @TableField("iLRMoney")
    @JsonProperty("iLRMoney")
    private BigDecimal iLRMoney;
    @TableField("iProxyADays")
    @JsonProperty("iProxyADays")
    private Integer iProxyADays;
    @TableField("iVenCreDate")
    @JsonProperty("iVenCreDate")
    private Integer iVenCreDate;
    @TableField("iVenCreGrade")
    @JsonProperty("iVenCreGrade")
    private String iVenCreGrade;
    @TableField("iVenCreLine")
    @JsonProperty("iVenCreLine")
    private BigDecimal iVenCreLine;
    @TableField("iVenDisRate")
    @JsonProperty("iVenDisRate")
    private BigDecimal iVenDisRate;
    @TableField("created_at")
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}
