package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发票摘要
 */
@Data
public class InvoiceSummaryVO {

    private String code;

    private String number;

    private String type;

    private String seller;

    @MoneyValue
    private BigDecimal amount;

    private String date;

    private String status;

    private String ocrStatus;
}
