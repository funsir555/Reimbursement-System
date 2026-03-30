package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseApprovalPendingItemVO {

    private Long taskId;

    private String documentCode;

    private String documentTitle;

    private String documentReason;

    private String templateName;

    private String submitterName;

    private Double amount;

    private String nodeKey;

    private String nodeName;

    private String status;

    private String submittedAt;

    private String taskCreatedAt;
}
