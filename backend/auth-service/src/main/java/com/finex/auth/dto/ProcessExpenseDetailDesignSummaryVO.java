package com.finex.auth.dto;

import lombok.Data;

@Data
public class ProcessExpenseDetailDesignSummaryVO {

    private Long id;

    private String detailCode;

    private String detailName;

    private String detailType;

    private String detailTypeLabel;

    private String detailDescription;

    private String updatedAt;
}
