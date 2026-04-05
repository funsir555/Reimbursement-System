package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseDetailInstanceSummaryVO {

    private String detailNo;

    private String detailDesignCode;

    private String detailType;

    private String detailTypeLabel;

    private String enterpriseMode;

    private String enterpriseModeLabel;

    private String expenseTypeCode;

    private String businessSceneMode;

    private String detailTitle;

    private Integer sortOrder;

    private String createdAt;
}
