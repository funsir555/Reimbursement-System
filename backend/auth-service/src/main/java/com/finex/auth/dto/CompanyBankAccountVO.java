package com.finex.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompanyBankAccountVO {

    private Long id;

    private String companyId;

    private String companyName;

    private String bankName;

    private String province;

    private String city;

    private String branchName;

    private String bankCode;

    private String branchCode;

    private String cnapsCode;

    private String accountName;

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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
