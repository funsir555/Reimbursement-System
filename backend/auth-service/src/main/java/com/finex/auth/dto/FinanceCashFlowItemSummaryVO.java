package com.finex.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FinanceCashFlowItemSummaryVO {

    private Long id;

    private String companyId;

    private String cashFlowCode;

    private String cashFlowName;

    private String direction;

    private Integer status;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
