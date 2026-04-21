package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExpenseApprovalNodeStatusVO {

    private String nodeKey;

    private String nodeName;

    private String nodeType;

    private String status;

    private String statusLabel;

    private List<String> assigneeNames = new ArrayList<>();

    private String occurredAt;

    private String description;
}
