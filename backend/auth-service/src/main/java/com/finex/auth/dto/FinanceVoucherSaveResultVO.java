package com.finex.auth.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FinanceVoucherSaveResultVO {

    private String voucherNo;

    private String companyId;

    private Integer iperiod;

    private String csign;

    private Integer inoId;

    private Integer entryCount;

    private BigDecimal totalDebit;

    private BigDecimal totalCredit;

    private String status;
}
