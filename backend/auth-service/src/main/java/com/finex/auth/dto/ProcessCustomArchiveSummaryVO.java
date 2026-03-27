package com.finex.auth.dto;

import lombok.Data;

@Data
public class ProcessCustomArchiveSummaryVO {

    private Long id;

    private String archiveCode;

    private String archiveName;

    private String archiveType;

    private String archiveTypeLabel;

    private String archiveDescription;

    private Integer status;

    private Integer itemCount;

    private String updatedAt;
}
