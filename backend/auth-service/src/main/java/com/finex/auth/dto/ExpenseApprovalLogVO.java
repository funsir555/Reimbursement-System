package com.finex.auth.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ExpenseApprovalLogVO {

    private Long id;

    private String documentCode;

    private String nodeKey;

    private String nodeName;

    private String actionType;

    private Long actorUserId;

    private String actorName;

    private String actionComment;

    private Map<String, Object> payload = new LinkedHashMap<>();

    private String createdAt;
}
