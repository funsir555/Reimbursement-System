package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinancePostVoucherTaskStatusVO {

    private String taskNo;

    private String taskType;

    private String businessType;

    private String status;

    private Integer progress;

    private String resultMessage;

    private String periodStatus;

    private String periodStatusLabel;

    private Integer postedVoucherCount;

    private Integer reviewableVoucherCount;

    private Boolean finished;

    private String createdAt;

    private String updatedAt;

    private String finishedAt;
}
