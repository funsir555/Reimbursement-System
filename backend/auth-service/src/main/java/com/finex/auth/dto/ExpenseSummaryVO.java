package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

import java.util.List;

@Data
public class ExpenseSummaryVO {

    private String documentCode;

    private String no;

    private String type;

    private String reason;

    private String documentTitle;

    private String documentReason;

    private String submitterName;

    private String submitterDeptName;

    private String templateName;

    private String templateType;

    private String templateTypeLabel;

    private String currentNodeName;

    private String documentStatus;

    private String documentStatusLabel;

    @MoneyValue
    private BigDecimal amount;

    @MoneyValue
    private BigDecimal outstandingAmount;

    private String date;

    private String status;

    private String submittedAt;

    private String paymentDate;

    private String paymentCompanyName;

    private String payeeName;

    private String counterpartyName;

    private List<String> undertakeDepartmentNames;

    private List<String> tagNames;
}
