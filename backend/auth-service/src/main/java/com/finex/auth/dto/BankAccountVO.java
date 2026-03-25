package com.finex.auth.dto;

import lombok.Data;

@Data
public class BankAccountVO {

    private Long id;

    private String bankName;

    private String branchName;

    private String accountName;

    private String accountNoMasked;

    private String accountType;

    private Boolean defaultAccount;

    private String status;
}
