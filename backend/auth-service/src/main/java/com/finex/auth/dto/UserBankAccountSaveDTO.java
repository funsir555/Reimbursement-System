package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserBankAccountSaveDTO {

    @NotBlank(message = "账户名不能为空")
    private String accountName;

    @NotBlank(message = "银行账号不能为空")
    private String accountNo;

    private String accountType;

    @NotBlank(message = "开户银行不能为空")
    private String bankName;

    @NotBlank(message = "开户银行编码不能为空")
    private String bankCode;

    @NotBlank(message = "开户省不能为空")
    private String province;

    @NotBlank(message = "开户市不能为空")
    private String city;

    @NotBlank(message = "开户网点不能为空")
    private String branchName;

    @NotBlank(message = "开户网点编码不能为空")
    private String branchCode;

    private String cnapsCode;

    private Integer defaultAccount;

    private Integer status;
}
