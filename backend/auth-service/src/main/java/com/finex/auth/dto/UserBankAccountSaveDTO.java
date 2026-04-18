package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserBankAccountSaveDTO {

    private static final String BANK_DIRECTORY_REQUIRED_MESSAGE = "请选择开户银行、开户省、开户市与开户网点后再保存";

    @NotBlank(message = "账户名不能为空")
    private String accountName;

    @NotBlank(message = "银行账号不能为空")
    private String accountNo;

    private String accountType;

    @NotBlank(message = BANK_DIRECTORY_REQUIRED_MESSAGE)
    private String bankName;

    @NotBlank(message = BANK_DIRECTORY_REQUIRED_MESSAGE)
    private String bankCode;

    @NotBlank(message = BANK_DIRECTORY_REQUIRED_MESSAGE)
    private String province;

    @NotBlank(message = BANK_DIRECTORY_REQUIRED_MESSAGE)
    private String city;

    @NotBlank(message = BANK_DIRECTORY_REQUIRED_MESSAGE)
    private String branchName;

    @NotBlank(message = BANK_DIRECTORY_REQUIRED_MESSAGE)
    private String branchCode;

    private String cnapsCode;

    private Integer defaultAccount;

    private Integer status;
}
