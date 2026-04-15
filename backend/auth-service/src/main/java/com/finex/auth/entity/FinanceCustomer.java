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
@TableName("gl_Customer")
public class FinanceCustomer {

    @TableId(value = "cCusCode", type = IdType.INPUT)
    @JsonProperty("cCusCode")
    private String cCusCode;

    @TableField("cCusName")
    @JsonProperty("cCusName")
    private String cCusName;
    @TableField("cCusAbbName")
    @JsonProperty("cCusAbbName")
    private String cCusAbbName;
    @TableField("cCCCode")
    @JsonProperty("cCCCode")
    private String cCCCode;
    @TableField("cDCCode")
    @JsonProperty("cDCCode")
    private String cDCCode;
    @TableField("cCusTradeCCode")
    @JsonProperty("cCusTradeCCode")
    private String cCusTradeCCode;
    @TableField("cTrade")
    @JsonProperty("cTrade")
    private String cTrade;
    @TableField("cCusAddress")
    @JsonProperty("cCusAddress")
    private String cCusAddress;
    @TableField("cCusPostCode")
    @JsonProperty("cCusPostCode")
    private String cCusPostCode;
    @TableField("cCusRegCode")
    @JsonProperty("cCusRegCode")
    private String cCusRegCode;
    @TableField("cCusBank")
    @JsonProperty("cCusBank")
    private String cCusBank;
    @TableField("cCusAccount")
    @JsonProperty("cCusAccount")
    private String cCusAccount;
    @TableField("cCusLPerson")
    @JsonProperty("cCusLPerson")
    private String cCusLPerson;
    @TableField("cCusPerson")
    @JsonProperty("cCusPerson")
    private String cCusPerson;
    @TableField("cCusHand")
    @JsonProperty("cCusHand")
    private String cCusHand;
    @TableField("cCusCreGrade")
    @JsonProperty("cCusCreGrade")
    private String cCusCreGrade;
    @TableField("iCusCreLine")
    @JsonProperty("iCusCreLine")
    private BigDecimal iCusCreLine;
    @TableField("iCusCreDate")
    @JsonProperty("iCusCreDate")
    private Integer iCusCreDate;
    @TableField("cCusOAddress")
    @JsonProperty("cCusOAddress")
    private String cCusOAddress;
    @TableField("cCusOType")
    @JsonProperty("cCusOType")
    private String cCusOType;
    @TableField("cCusHeadCode")
    @JsonProperty("cCusHeadCode")
    private String cCusHeadCode;
    @TableField("cCusWhCode")
    @JsonProperty("cCusWhCode")
    private String cCusWhCode;
    @TableField("cCusDepart")
    @JsonProperty("cCusDepart")
    private String cCusDepart;
    @TableField("iARMoney")
    @JsonProperty("iARMoney")
    private BigDecimal iARMoney;
    @TableField("dLastDate")
    @JsonProperty("dLastDate")
    private LocalDateTime dLastDate;
    @TableField("iLastMoney")
    @JsonProperty("iLastMoney")
    private BigDecimal iLastMoney;
    @TableField("dLRDate")
    @JsonProperty("dLRDate")
    private LocalDateTime dLRDate;
    @TableField("iLRMoney")
    @JsonProperty("iLRMoney")
    private BigDecimal iLRMoney;
    @TableField("dEndDate")
    @JsonProperty("dEndDate")
    private LocalDateTime dEndDate;
    @TableField("cCusBankCode")
    @JsonProperty("cCusBankCode")
    private String cCusBankCode;
    @TableField("cCusDefine1")
    @JsonProperty("cCusDefine1")
    private String cCusDefine1;
    @TableField("cCusDefine2")
    @JsonProperty("cCusDefine2")
    private String cCusDefine2;
    @TableField("cCusDefine3")
    @JsonProperty("cCusDefine3")
    private String cCusDefine3;
    @TableField("cCusDefine4")
    @JsonProperty("cCusDefine4")
    private String cCusDefine4;
    @TableField("cCusDefine5")
    @JsonProperty("cCusDefine5")
    private String cCusDefine5;
    @TableField("cCusDefine6")
    @JsonProperty("cCusDefine6")
    private String cCusDefine6;
    @TableField("cCusDefine7")
    @JsonProperty("cCusDefine7")
    private String cCusDefine7;
    @TableField("cCusDefine8")
    @JsonProperty("cCusDefine8")
    private String cCusDefine8;
    @TableField("cCusDefine9")
    @JsonProperty("cCusDefine9")
    private String cCusDefine9;
    @TableField("cCusDefine10")
    @JsonProperty("cCusDefine10")
    private String cCusDefine10;
    @TableField("cCusDefine11")
    @JsonProperty("cCusDefine11")
    private Integer cCusDefine11;
    @TableField("cCusDefine12")
    @JsonProperty("cCusDefine12")
    private Integer cCusDefine12;
    @TableField("cCusDefine13")
    @JsonProperty("cCusDefine13")
    private BigDecimal cCusDefine13;
    @TableField("cCusDefine14")
    @JsonProperty("cCusDefine14")
    private BigDecimal cCusDefine14;
    @TableField("cCusDefine15")
    @JsonProperty("cCusDefine15")
    private LocalDateTime cCusDefine15;
    @TableField("cCusDefine16")
    @JsonProperty("cCusDefine16")
    private LocalDateTime cCusDefine16;
    @TableField("cInvoiceCompany")
    @JsonProperty("cInvoiceCompany")
    private String cInvoiceCompany;
    @TableField("bCredit")
    @JsonProperty("bCredit")
    private Integer bCredit;
    @TableField("bCreditDate")
    @JsonProperty("bCreditDate")
    private Integer bCreditDate;
    @TableField("bCreditByHead")
    @JsonProperty("bCreditByHead")
    private Integer bCreditByHead;
    @TableField("cMemo")
    @JsonProperty("cMemo")
    private String cMemo;
    @TableField("fCommisionRate")
    @JsonProperty("fCommisionRate")
    private BigDecimal fCommisionRate;
    @TableField("fInsueRate")
    @JsonProperty("fInsueRate")
    private BigDecimal fInsueRate;
    @TableField("CustomerKCode")
    @JsonProperty("customerKCode")
    private String customerKCode;
    @TableField("bCusState")
    @JsonProperty("bCusState")
    private Integer bCusState;
    @TableField("company_id")
    @JsonProperty("companyId")
    private String companyId;
    @TableField("created_at")
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}
