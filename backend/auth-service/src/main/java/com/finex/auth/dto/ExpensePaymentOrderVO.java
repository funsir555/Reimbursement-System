package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpensePaymentOrderVO {

    private Long taskId;

    private String documentCode;

    private String documentTitle;

    private String templateName;

    private String templateType;

    private String templateTypeLabel;

    private String submitterName;

    private String submitterDeptName;

    private String currentNodeName;

    private String documentStatus;

    private String documentStatusLabel;

    @MoneyValue
    private BigDecimal amount;

    private String submittedAt;

    private String paymentDate;

    private String paymentCompanyName;

    private String paymentStatusCode;

    private String paymentStatusLabel;

    private boolean manualPaid;

    private String paidAt;

    private String receiptStatusLabel;

    private String receiptReceivedAt;

    private String bankFlowNo;

    private String companyBankAccountName;

    private String taskCreatedAt;

    private boolean allowRetry;

    private String payeeOrCounterpartyName;

    private String payeeAccountNo;

    private String payeeBankName;
}
