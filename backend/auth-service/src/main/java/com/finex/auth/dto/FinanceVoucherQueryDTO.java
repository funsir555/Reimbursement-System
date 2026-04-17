package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceVoucherQueryDTO {

    private String companyId;

    private String voucherNo;

    private String status;

    private String csign;

    private String billMonth;

    private String billMonthFrom;

    private String billMonthTo;

    private String summary;

    private Integer page;

    private Integer pageSize;
}
