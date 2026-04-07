package com.finex.auth.dto;

import lombok.Data;

@Data
public class ArchiveAgentVersionVO {

    private Long id;

    private Integer versionNo;

    private String versionLabel;

    private Boolean published;

    private String createdByName;

    private String createdAt;
}
