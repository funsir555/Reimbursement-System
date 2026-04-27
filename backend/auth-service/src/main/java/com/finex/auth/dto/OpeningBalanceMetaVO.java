package com.finex.auth.dto;

import java.util.List;
import lombok.Data;

@Data
public class OpeningBalanceMetaVO {

    private List<FinanceVoucherOptionVO> companyOptions;

    private List<FinanceVoucherOptionVO> departmentOptions;

    private List<FinanceVoucherOptionVO> employeeOptions;

    private List<FinanceVoucherOptionVO> customerOptions;

    private List<FinanceVoucherOptionVO> supplierOptions;

    private List<FinanceVoucherOptionVO> projectClassOptions;

    private List<FinanceVoucherOptionVO> projectOptions;

    private String defaultCompanyId;

    private Integer defaultYear;

    private Integer defaultPeriod;

    private Integer defaultYearPeriod;

    private String status;

    private String statusLabel;

    private Boolean opened;
}
