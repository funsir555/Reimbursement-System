package com.finex.auth.dto;

import lombok.Data;

/**
 * 审批摘要
 */
@Data
public class ApprovalSummaryVO {

    private Long id;

    private String title;

    private String submitter;

    private String time;

    private Double amount;

    private String avatar;
}
