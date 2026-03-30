package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseDocumentSubmitResultVO {

    private Long id;
    private String documentCode;
    private String status;
}
