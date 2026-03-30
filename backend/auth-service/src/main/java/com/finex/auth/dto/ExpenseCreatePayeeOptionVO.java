package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseCreatePayeeOptionVO {

    private String value;
    private String label;
    private String sourceType;
    private String sourceCode;
    private String secondaryLabel;
}
