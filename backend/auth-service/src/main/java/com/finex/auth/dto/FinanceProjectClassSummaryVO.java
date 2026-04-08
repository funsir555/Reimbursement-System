package com.finex.auth.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FinanceProjectClassSummaryVO {

    private Long id;

    private String companyId;

    private String projectClassCode;

    private String projectClassName;

    private Integer status;

    private Integer sortOrder;

    private Boolean hasProjects;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
