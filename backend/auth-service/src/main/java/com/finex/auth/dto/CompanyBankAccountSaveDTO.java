package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompanyBankAccountSaveDTO {

    private static final String BANK_DIRECTORY_REQUIRED_MESSAGE = "请选择开户银行、开户省、开户市与开户网点后再保存";

    @NotBlank(message = "\u516c\u53f8\u4e0d\u80fd\u4e3a\u7a7a")
    private String companyId;

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

    @NotBlank(message = "\u8d26\u6237\u540d\u4e0d\u80fd\u4e3a\u7a7a")
    private String accountName;

    @NotBlank(message = "\u94f6\u884c\u8d26\u53f7\u4e0d\u80fd\u4e3a\u7a7a")
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
