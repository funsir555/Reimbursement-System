package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceVoucherOptionVO {

    private String value;

    private String code;

    private String name;

    private String label;

    private String parentValue;

    private Integer bperson;

    private Integer bcus;

    private Integer bsup;

    private Integer bdept;

    private Integer bitem;

    private String cassItem;
}
