package com.finex.auth.dto;

import lombok.Data;

@Data
public class ProcessFlowSummaryVO {

    private Long id;

    private String flowCode;

    private String flowName;

    private String flowDescription;

    private String status;

    private String statusLabel;

    private Integer currentVersionNo;

    private String updatedAt;
}
