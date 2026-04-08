package com.finex.auth.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FinanceProjectSummaryVO {

    private Long id;

    private String companyId;

    private String citemcode;

    private String citemname;

    private Integer bclose;

    private String citemccode;

    private String projectClassName;

    private Integer iotherused;

    private LocalDateTime dEndDate;

    private Integer status;

    private Integer sortOrder;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Boolean referencedByVoucher;
}
