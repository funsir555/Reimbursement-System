package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceContextCompanyOptionVO {

    private String companyId;

    private String companyCode;

    private String companyName;

    private boolean hasActiveAccountSet;

    private Integer enabledYear;

    private Integer enabledPeriod;

    private Integer periodStartYear;

    private Integer periodStartMonth;

    private Integer periodEndYear;

    private Integer periodEndMonth;

    private String value;

    private String label;
}
