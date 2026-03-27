package com.finex.auth.dto;

import lombok.Data;

/**
 * 发票摘要
 */
@Data
public class InvoiceSummaryVO {

    private String code;

    private String number;

    private String type;

    private String seller;

    private Double amount;

    private String date;

    private String status;

    private String ocrStatus;
}
