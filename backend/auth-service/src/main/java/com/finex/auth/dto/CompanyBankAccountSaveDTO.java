package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompanyBankAccountSaveDTO {

    @NotBlank(message = "Company is required")
    private String companyId;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "Bank code is required")
    private String bankCode;

    @NotBlank(message = "Province is required")
    private String province;

    @NotBlank(message = "City is required")
    private String city;

    private String branchName;

    @NotBlank(message = "Branch code is required")
    private String branchCode;

    private String cnapsCode;

    @NotBlank(message = "Account name is required")
    private String accountName;

    @NotBlank(message = "Account number is required")
    private String accountNo;

    private String accountType;

    private String accountUsage;

    private String currencyCode;

    private Integer defaultAccount;

    private Integer status;

    private String remark;

    private Integer directConnectEnabled;

    private String directConnectProvider;

    private String directConnectChannel;

    private String directConnectProtocol;

    private String directConnectCustomerNo;

    private String directConnectAppId;

    private String directConnectAccountAlias;

    private String directConnectAuthMode;

    private String directConnectApiBaseUrl;

    private String directConnectCertRef;

    private String directConnectSecretRef;

    private String directConnectSignType;

    private String directConnectEncryptType;

    private LocalDateTime directConnectLastSyncAt;

    private String directConnectLastSyncStatus;

    private String directConnectLastErrorMsg;

    private String directConnectExtJson;
}
