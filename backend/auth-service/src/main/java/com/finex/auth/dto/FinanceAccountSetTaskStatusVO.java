package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceAccountSetTaskStatusVO {

    private String taskNo;

    private String companyId;

    private String taskType;

    private String status;

    private Integer progress;

    private String resultMessage;

    private String accountSetStatus;

    private Boolean finished;

    private String createdAt;

    private String updatedAt;

    private String finishedAt;
}
