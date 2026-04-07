package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyInput;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinanceCustomerSaveDTO {

    private String cCusCode;
    private String cCusName;
    private String cCusAbbName;
    private String cCCCode;
    private String cDCCode;
    private String cCusTradeCCode;
    private String cTrade;
    private String cCusAddress;
    private String cCusPostCode;
    private String cCusRegCode;
    private String cCusBank;
    private String cCusAccount;
    private String cCusLPerson;
    private String cCusPerson;
    private String cCusHand;
    private String cCusCreGrade;
    @MoneyInput
    private BigDecimal iCusCreLine;
    private Integer iCusCreDate;
    private String cCusOAddress;
    private String cCusOType;
    private String cCusHeadCode;
    private String cCusWhCode;
    private String cCusDepart;
    @MoneyInput
    private BigDecimal iARMoney;
    private LocalDateTime dLastDate;
    @MoneyInput
    private BigDecimal iLastMoney;
    private LocalDateTime dLRDate;
    @MoneyInput
    private BigDecimal iLRMoney;
    private LocalDateTime dEndDate;
    private String cCusBankCode;
    private String cCusDefine1;
    private String cCusDefine2;
    private String cCusDefine3;
    private String cCusDefine4;
    private String cCusDefine5;
    private String cCusDefine6;
    private String cCusDefine7;
    private String cCusDefine8;
    private String cCusDefine9;
    private String cCusDefine10;
    private Integer cCusDefine11;
    private Integer cCusDefine12;
    private BigDecimal cCusDefine13;
    private BigDecimal cCusDefine14;
    private LocalDateTime cCusDefine15;
    private LocalDateTime cCusDefine16;
    private String cInvoiceCompany;
    private Integer bCredit;
    private Integer bCreditDate;
    private Integer bCreditByHead;
    private String cMemo;
    private BigDecimal fCommisionRate;
    private BigDecimal fInsueRate;
    private String customerKCode;
    private Integer bCusState;
    private String companyId;
}
