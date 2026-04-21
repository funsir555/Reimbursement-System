package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExpenseApprovalTimelineItemVO {

    private String key;

    private String nodeKey;

    private String nodeName;

    private String nodeType;

    private String status;

    private String statusLabel;

    private String title;

    private String description;

    private String timestamp;

    private boolean pending;

    private boolean future;

    private List<String> attachmentNames = new ArrayList<>();
}
