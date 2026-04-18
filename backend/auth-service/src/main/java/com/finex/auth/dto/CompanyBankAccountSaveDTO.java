package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompanyBankAccountSaveDTO {

    @NotBlank(message = "\u516c\u53f8\u4e0d\u80fd\u4e3a\u7a7a")
    private String companyId;

    @NotBlank(message = "\u5f00\u6237\u94f6\u884c\u4e0d\u80fd\u4e3a\u7a7a")
    private String bankName;

    @NotBlank(message = "\u5f00\u6237\u94f6\u884c\u7f16\u7801\u4e0d\u80fd\u4e3a\u7a7a")
    private String bankCode;

    @NotBlank(message = "\u5f00\u6237\u7701\u4e0d\u80fd\u4e3a\u7a7a")
    private String province;

    @NotBlank(message = "\u5f00\u6237\u5e02\u4e0d\u80fd\u4e3a\u7a7a")
    private String city;

    private String branchName;

    @NotBlank(message = "\u5f00\u6237\u7f51\u70b9\u7f16\u7801\u4e0d\u80fd\u4e3a\u7a7a")
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
