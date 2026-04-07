package com.finex.auth.entity;

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
    private String cCusCode;

    @TableField("cCusName")
    private String cCusName;
    @TableField("cCusAbbName")
    private String cCusAbbName;
    @TableField("cCCCode")
    private String cCCCode;
    @TableField("cDCCode")
    private String cDCCode;
    @TableField("cCusTradeCCode")
    private String cCusTradeCCode;
    @TableField("cTrade")
    private String cTrade;
    @TableField("cCusAddress")
    private String cCusAddress;
    @TableField("cCusPostCode")
    private String cCusPostCode;
    @TableField("cCusRegCode")
    private String cCusRegCode;
    @TableField("cCusBank")
    private String cCusBank;
    @TableField("cCusAccount")
    private String cCusAccount;
    @TableField("cCusLPerson")
    private String cCusLPerson;
    @TableField("cCusPerson")
    private String cCusPerson;
    @TableField("cCusHand")
    private String cCusHand;
    @TableField("cCusCreGrade")
    private String cCusCreGrade;
    @TableField("iCusCreLine")
    private BigDecimal iCusCreLine;
    @TableField("iCusCreDate")
    private Integer iCusCreDate;
    @TableField("cCusOAddress")
    private String cCusOAddress;
    @TableField("cCusOType")
    private String cCusOType;
    @TableField("cCusHeadCode")
    private String cCusHeadCode;
    @TableField("cCusWhCode")
    private String cCusWhCode;
    @TableField("cCusDepart")
    private String cCusDepart;
    @TableField("iARMoney")
    private BigDecimal iARMoney;
    @TableField("dLastDate")
    private LocalDateTime dLastDate;
    @TableField("iLastMoney")
    private BigDecimal iLastMoney;
    @TableField("dLRDate")
    private LocalDateTime dLRDate;
    @TableField("iLRMoney")
    private BigDecimal iLRMoney;
    @TableField("dEndDate")
    private LocalDateTime dEndDate;
    @TableField("cCusBankCode")
    private String cCusBankCode;
    @TableField("cCusDefine1")
    private String cCusDefine1;
    @TableField("cCusDefine2")
    private String cCusDefine2;
    @TableField("cCusDefine3")
    private String cCusDefine3;
    @TableField("cCusDefine4")
    private String cCusDefine4;
    @TableField("cCusDefine5")
    private String cCusDefine5;
    @TableField("cCusDefine6")
    private String cCusDefine6;
    @TableField("cCusDefine7")
    private String cCusDefine7;
    @TableField("cCusDefine8")
    private String cCusDefine8;
    @TableField("cCusDefine9")
    private String cCusDefine9;
    @TableField("cCusDefine10")
    private String cCusDefine10;
    @TableField("cCusDefine11")
    private Integer cCusDefine11;
    @TableField("cCusDefine12")
    private Integer cCusDefine12;
    @TableField("cCusDefine13")
    private BigDecimal cCusDefine13;
    @TableField("cCusDefine14")
    private BigDecimal cCusDefine14;
    @TableField("cCusDefine15")
    private LocalDateTime cCusDefine15;
    @TableField("cCusDefine16")
    private LocalDateTime cCusDefine16;
    @TableField("cInvoiceCompany")
    private String cInvoiceCompany;
    @TableField("bCredit")
    private Integer bCredit;
    @TableField("bCreditDate")
    private Integer bCreditDate;
    @TableField("bCreditByHead")
    private Integer bCreditByHead;
    @TableField("cMemo")
    private String cMemo;
    @TableField("fCommisionRate")
    private BigDecimal fCommisionRate;
    @TableField("fInsueRate")
    private BigDecimal fInsueRate;
    @TableField("CustomerKCode")
    private String customerKCode;
    @TableField("bCusState")
    private Integer bCusState;
    @TableField("company_id")
    private String companyId;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
