package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseDocumentBankPaymentVO {

    private String bankProvider;

    private String bankChannel;

    private String companyBankAccountName;

    private String paymentStatusCode;

    private String paymentStatusLabel;

    private boolean manualPaid;

    private String paidAt;

    private String receiptStatusLabel;

    private String receiptReceivedAt;

    private String bankFlowNo;

    private String bankOrderNo;

    private String lastErrorMessage;
}
