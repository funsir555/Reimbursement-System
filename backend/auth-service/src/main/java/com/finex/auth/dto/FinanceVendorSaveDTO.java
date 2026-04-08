package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyInput;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinanceVendorSaveDTO {

    private String cVenCode;
    private String cVenName;
    private String cVenAbbName;
    private String cVCCode;
    private String cTrade;
    private String cVenAddress;
    private String cVenRegCode;
    private String cVenBank;
    private String cVenAccount;
    private String cVenBankNub;
    private String receiptAccountName;
    private String receiptBankProvince;
    private String receiptBankCity;
    private String receiptBranchCode;
    private String receiptBranchName;
    private String cVenPerson;
    private String cVenPhone;
    private String cVenHand;
    private String cVenEmail;
    private String companyId;
    private String cMemo;
    private LocalDateTime dEndDate;
    private Integer bBusinessDate;
    private Integer bLicenceDate;
    private Integer bPassGMP;
    private Integer bProxyDate;
    private Integer bProxyForeign;
    private Integer bVenCargo;
    private Integer bVenService;
    private Integer bVenTax;
    private String cBarCode;
    private String cCreatePerson;
    private String cDCCode;
    private String cModifyPerson;
    private String cRelCustomer;
    private String cVenBankCode;
    private String cVenBP;
    private String cVenDefine10;
    private Integer cVenDefine11;
    private Integer cVenDefine12;
    private BigDecimal cVenDefine13;
    private BigDecimal cVenDefine14;
    private LocalDateTime cVenDefine15;
    private LocalDateTime cVenDefine16;
    private String cVenDefine3;
    private String cVenDefine4;
    private String cVenDefine5;
    private String cVenDefine6;
    private String cVenDefine7;
    private String cVenDefine8;
    private String cVenDefine9;
    private String cVenDepart;
    private String cVenFax;
    private String cVenHeadCode;
    private String cVenIAddress;
    private String cVenIType;
    private String cVenLPerson;
    private String cVenPayCond;
    private String cVenPostCode;
    private String cVenPPerson;
    private String cVenTradeCCode;
    private String cVenWhCode;
    private LocalDateTime dBusinessEDate;
    private LocalDateTime dBusinessSDate;
    private LocalDateTime dLastDate;
    private LocalDateTime dLicenceEDate;
    private LocalDateTime dLicenceSDate;
    private LocalDateTime dLRDate;
    private LocalDateTime dModifyDate;
    private LocalDateTime dProxyEDate;
    private LocalDateTime dProxySDate;
    private LocalDateTime dVenDevDate;
    @MoneyInput
    private BigDecimal fRegistFund;
    @MoneyInput
    private BigDecimal iAPMoney;
    private Integer iBusinessADays;
    private Integer iEmployeeNum;
    private Integer iFrequency;
    private Integer iGradeABC;
    private Integer iId;
    @MoneyInput
    private BigDecimal iLastMoney;
    private Integer iLicenceADays;
    @MoneyInput
    private BigDecimal iLRMoney;
    private Integer iProxyADays;
    private Integer iVenCreDate;
    private String iVenCreGrade;
    @MoneyInput
    private BigDecimal iVenCreLine;
    private BigDecimal iVenDisRate;
}
