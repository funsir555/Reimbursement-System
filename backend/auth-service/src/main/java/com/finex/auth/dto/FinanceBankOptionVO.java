package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceBankOptionVO {

    private String bankCode;

    private String bankName;

    private String value;

    private String label;
}
