package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseBankLinkConfigVO {

    private Long companyBankAccountId;

    private String companyId;

    private String companyName;

    private String accountName;

    private String accountNo;

    private String bankName;

    private Integer accountStatus;

    private boolean directConnectEnabled;

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

    private String operatorKey;

    private String callbackSecret;

    private String publicKeyRef;

    private boolean receiptQueryEnabled;

    private String lastDirectConnectStatus;

    private String lastDirectConnectError;
}
