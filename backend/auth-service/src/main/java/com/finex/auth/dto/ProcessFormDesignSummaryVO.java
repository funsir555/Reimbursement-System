package com.finex.auth.dto;

import lombok.Data;

@Data
public class ProcessFormDesignSummaryVO {

    private Long id;

    private String formCode;

    private String formName;

    private String templateType;

    private String templateTypeLabel;

    private String formDescription;

    private String updatedAt;
}
