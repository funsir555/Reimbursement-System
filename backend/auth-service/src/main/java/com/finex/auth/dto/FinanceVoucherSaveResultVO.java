package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
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

    @MoneyValue
    private BigDecimal totalDebit;

    @MoneyValue
    private BigDecimal totalCredit;

    private String status;
}
