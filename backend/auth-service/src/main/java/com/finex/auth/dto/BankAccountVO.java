package com.finex.auth.dto;

import lombok.Data;

@Data
public class BankAccountVO {

    private Long id;

    private String bankCode;

    private String bankName;

    private String province;

    private String city;

    private String branchCode;

    private String branchName;

    private String cnapsCode;

    private String accountName;

    private String accountNo;

    private String accountNoMasked;

    private String accountType;

    private Boolean defaultAccount;

    private Integer status;

    private String statusLabel;

    private String createdAt;

    private String updatedAt;
}
