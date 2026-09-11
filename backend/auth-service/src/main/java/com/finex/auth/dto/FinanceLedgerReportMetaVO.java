package com.finex.auth.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class FinanceLedgerReportMetaVO {

    private List<FinanceContextCompanyOptionVO> companyOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> departmentOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> employeeOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> makerOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> voucherTypeOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> accountOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> customerOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> supplierOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> projectClassOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> projectOptions = new ArrayList<>();

    private String defaultCompanyId;

    private Integer defaultYear;

    private Integer defaultPeriod;

    private Integer defaultYearPeriod;

    private Integer periodStartYear;

    private Integer periodStartMonth;

    private Integer periodEndYear;

    private Integer periodEndMonth;
}
