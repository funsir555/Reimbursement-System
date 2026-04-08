package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceBankBranchVO {

    private Long id;

    private String bankCode;

    private String bankName;

    private String province;

    private String city;

    private String branchCode;

    private String branchName;

    private String cnapsCode;

    private String value;

    private String label;
}
