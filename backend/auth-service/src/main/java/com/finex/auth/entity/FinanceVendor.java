package com.finex.auth.entity;

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
    private String cVenCode;

    @TableField("cVenName")
    private String cVenName;
    @TableField("cVenAbbName")
    private String cVenAbbName;
    @TableField("cVCCode")
    private String cVCCode;
    @TableField("cTrade")
    private String cTrade;
    @TableField("cVenAddress")
    private String cVenAddress;
    @TableField("cVenRegCode")
    private String cVenRegCode;
    @TableField("cVenBank")
    private String cVenBank;
    @TableField("cVenAccount")
    private String cVenAccount;
    @TableField("cVenBankNub")
    private String cVenBankNub;
    @TableField("cVenPerson")
    private String cVenPerson;
    @TableField("cVenPhone")
    private String cVenPhone;
    @TableField("cVenHand")
    private String cVenHand;
    @TableField("cVenEmail")
    private String cVenEmail;

    @TableField("company_id")
    private String companyId;

    @TableField("cMemo")
    private String cMemo;
    @TableField("dEndDate")
    private LocalDateTime dEndDate;
    @TableField("bBusinessDate")
    private Integer bBusinessDate;
    @TableField("bLicenceDate")
    private Integer bLicenceDate;
    @TableField("bPassGMP")
    private Integer bPassGMP;
    @TableField("bProxyDate")
    private Integer bProxyDate;
    @TableField("bProxyForeign")
    private Integer bProxyForeign;
    @TableField("bVenCargo")
    private Integer bVenCargo;
    @TableField("bVenService")
    private Integer bVenService;
    @TableField("bVenTax")
    private Integer bVenTax;
    @TableField("cBarCode")
    private String cBarCode;
    @TableField("cCreatePerson")
    private String cCreatePerson;
    @TableField("cDCCode")
    private String cDCCode;
    @TableField("cModifyPerson")
    private String cModifyPerson;
    @TableField("cRelCustomer")
    private String cRelCustomer;
    @TableField("cVenBankCode")
    private String cVenBankCode;
    @TableField("cVenBP")
    private String cVenBP;
    @TableField("cVenDefine10")
    private String cVenDefine10;
    @TableField("cVenDefine11")
    private Integer cVenDefine11;
    @TableField("cVenDefine12")
    private Integer cVenDefine12;
    @TableField("cVenDefine13")
    private BigDecimal cVenDefine13;
    @TableField("cVenDefine14")
    private BigDecimal cVenDefine14;
    @TableField("cVenDefine15")
    private LocalDateTime cVenDefine15;
    @TableField("cVenDefine16")
    private LocalDateTime cVenDefine16;
    @TableField("cVenDefine3")
    private String cVenDefine3;
    @TableField("cVenDefine4")
    private String cVenDefine4;
    @TableField("cVenDefine5")
    private String cVenDefine5;
    @TableField("cVenDefine6")
    private String cVenDefine6;
    @TableField("cVenDefine7")
    private String cVenDefine7;
    @TableField("cVenDefine8")
    private String cVenDefine8;
    @TableField("cVenDefine9")
    private String cVenDefine9;
    @TableField("cVenDepart")
    private String cVenDepart;
    @TableField("cVenFax")
    private String cVenFax;
    @TableField("cVenHeadCode")
    private String cVenHeadCode;
    @TableField("cVenIAddress")
    private String cVenIAddress;
    @TableField("cVenIType")
    private String cVenIType;
    @TableField("cVenLPerson")
    private String cVenLPerson;
    @TableField("cVenPayCond")
    private String cVenPayCond;
    @TableField("cVenPostCode")
    private String cVenPostCode;
    @TableField("cVenPPerson")
    private String cVenPPerson;
    @TableField("cVenTradeCCode")
    private String cVenTradeCCode;
    @TableField("cVenWhCode")
    private String cVenWhCode;
    @TableField("dBusinessEDate")
    private LocalDateTime dBusinessEDate;
    @TableField("dBusinessSDate")
    private LocalDateTime dBusinessSDate;
    @TableField("dLastDate")
    private LocalDateTime dLastDate;
    @TableField("dLicenceEDate")
    private LocalDateTime dLicenceEDate;
    @TableField("dLicenceSDate")
    private LocalDateTime dLicenceSDate;
    @TableField("dLRDate")
    private LocalDateTime dLRDate;
    @TableField("dModifyDate")
    private LocalDateTime dModifyDate;
    @TableField("dProxyEDate")
    private LocalDateTime dProxyEDate;
    @TableField("dProxySDate")
    private LocalDateTime dProxySDate;
    @TableField("dVenDevDate")
    private LocalDateTime dVenDevDate;
    @TableField("fRegistFund")
    private BigDecimal fRegistFund;
    @TableField("iAPMoney")
    private BigDecimal iAPMoney;
    @TableField("iBusinessADays")
    private Integer iBusinessADays;
    @TableField("iEmployeeNum")
    private Integer iEmployeeNum;
    @TableField("iFrequency")
    private Integer iFrequency;
    @TableField("iGradeABC")
    private Integer iGradeABC;
    @TableField("iId")
    private Integer iId;
    @TableField("iLastMoney")
    private BigDecimal iLastMoney;
    @TableField("iLicenceADays")
    private Integer iLicenceADays;
    @TableField("iLRMoney")
    private BigDecimal iLRMoney;
    @TableField("iProxyADays")
    private Integer iProxyADays;
    @TableField("iVenCreDate")
    private Integer iVenCreDate;
    @TableField("iVenCreGrade")
    private String iVenCreGrade;
    @TableField("iVenCreLine")
    private BigDecimal iVenCreLine;
    @TableField("iVenDisRate")
    private BigDecimal iVenDisRate;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
