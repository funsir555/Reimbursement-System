package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseApprovalActionDTO {

    private String comment;

    private String targetNodeKey;
}
