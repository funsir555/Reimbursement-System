package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceCloseLedgerCheckItemVO {

    private String code;

    private String label;

    private Boolean passed;

    private String message;
}
