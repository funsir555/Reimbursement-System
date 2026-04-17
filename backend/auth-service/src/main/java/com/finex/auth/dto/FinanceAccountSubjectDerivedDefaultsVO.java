package com.finex.auth.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FinanceAccountSubjectDerivedDefaultsVO {

    private String parentSubjectCode;

    private Integer subjectLevel;

    private String subjectCategory;

    private String balanceDirection;

    private Integer leafFlag;

    private String matchedBy;
}
