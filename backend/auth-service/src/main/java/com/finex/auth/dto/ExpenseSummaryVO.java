package com.finex.auth.dto;

import lombok.Data;

/**
 * 报销摘要
 */
@Data
public class ExpenseSummaryVO {

    private String no;

    private String type;

    private String reason;

    private Double amount;

    private String date;

    private String status;
}
