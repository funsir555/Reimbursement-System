package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

import java.util.List;

@Data
public class ExpenseApprovalPendingItemVO {

    private Long taskId;

    private String documentCode;

    private String documentTitle;

    private String documentReason;

    private String templateName;

    private String templateType;

    private String templateTypeLabel;

    private String submitterName;

    private String submitterDeptName;

    @MoneyValue
    private BigDecimal amount;

    private String nodeKey;

    private String nodeName;

    private String status;

    private String documentStatus;

    private String documentStatusLabel;

    private String submittedAt;

    private String paymentDate;

    private String paymentCompanyName;

    private String payeeName;

    private String counterpartyName;

    private List<String> undertakeDepartmentNames;

    private List<String> tagNames;

    private String taskCreatedAt;
}
