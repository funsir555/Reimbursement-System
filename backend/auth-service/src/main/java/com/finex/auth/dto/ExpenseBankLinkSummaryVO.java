package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseBankLinkSummaryVO {

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

    private String directConnectStatusLabel;

    private String lastDirectConnectStatus;

    private String lastReceiptStatus;
}
