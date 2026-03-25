package com.finex.auth.dto;

import lombok.Data;

@Data
public class DownloadRecordVO {

    private Long id;

    private String fileName;

    private String businessType;

    private String status;

    private Integer progress;

    private String fileSize;

    private String createdAt;

    private String finishedAt;
}
