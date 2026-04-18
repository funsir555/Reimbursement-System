package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceVoucherMetaVO {

    private List<FinanceVoucherOptionVO> companyOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> departmentOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> employeeOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> voucherTypeOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> currencyOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> accountOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> customerOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> supplierOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> projectClassOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> projectOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> cashFlowOptions = new ArrayList<>();

    private String defaultCompanyId;

    private String defaultBillDate;

    private Integer defaultPeriod;

    private String defaultVoucherType;

    private Integer suggestedVoucherNo;

    private String defaultMaker;

    private Integer defaultAttachedDocCount;

    private String defaultCurrency;
}
