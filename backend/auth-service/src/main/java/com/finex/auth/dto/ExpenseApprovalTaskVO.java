package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseApprovalTaskVO {

    private Long id;

    private String documentCode;

    private String nodeKey;

    private String nodeName;

    private String nodeType;

    private Long assigneeUserId;

    private String assigneeName;

    private String status;

    private String taskBatchNo;

    private String approvalMode;

    private String taskKind;

    private Long sourceTaskId;

    private String actionComment;

    private String createdAt;

    private String handledAt;
}
