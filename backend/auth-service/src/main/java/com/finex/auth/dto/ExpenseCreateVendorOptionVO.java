package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseCreateVendorOptionVO {

    private String value;
    private String label;
    private String secondaryLabel;
    private String cVenCode;
    private String cVenName;
    private String cVenAbbName;
}
