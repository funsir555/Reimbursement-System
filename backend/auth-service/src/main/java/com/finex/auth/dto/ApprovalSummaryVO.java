package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 审批摘要
 */
@Data
public class ApprovalSummaryVO {

    private Long id;

    private String title;

    private String submitter;

    private String time;

    @MoneyValue
    private BigDecimal amount;

    private String avatar;
}
