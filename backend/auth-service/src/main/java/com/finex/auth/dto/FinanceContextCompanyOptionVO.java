package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceContextCompanyOptionVO {

    private String companyId;

    private String companyCode;

    private String companyName;

    private boolean hasActiveAccountSet;

    private String value;

    private String label;
}
