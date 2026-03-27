package com.finex.auth.dto;

import lombok.Data;

@Data
public class AsyncTaskSubmitResultVO {

    private String taskNo;

    private String taskType;

    private String businessType;

    private String status;

    private String message;

    private Long downloadRecordId;
}
