package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseCreatePayeeAccountOptionVO {

    private String value;
    private String label;
    private String sourceType;
    private String ownerCode;
    private String ownerName;
    private String bankName;
    private String accountName;
    private String accountNoMasked;
    private String secondaryLabel;
}
