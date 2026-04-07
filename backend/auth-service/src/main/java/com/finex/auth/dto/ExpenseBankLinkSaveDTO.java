package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExpenseBankLinkSaveDTO {

    private Boolean enabled;

    @NotBlank(message = "银行提供方不能为空")
    private String directConnectProvider;

    @NotBlank(message = "直连通道不能为空")
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

    private Boolean receiptQueryEnabled;
}
