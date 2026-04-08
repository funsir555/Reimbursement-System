package com.finex.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FinanceAccountSubjectSummaryVO {

    private String subjectCode;

    private String subjectName;

    private String parentSubjectCode;

    private Integer subjectLevel;

    private String balanceDirection;

    private String subjectCategory;

    private String chelp;

    private Integer leafFlag;

    private Integer status;

    private Integer bclose;

    private Integer bperson;

    private Integer bcus;

    private Integer bsup;

    private Integer bdept;

    private Integer bitem;

    private Integer bcash;

    private Integer bbank;

    private Integer br;

    private Integer be;

    private String auxiliarySummary;

    private String cashBankSummary;

    private Boolean hasChildren;

    private String templateCode;

    private Integer sortOrder;

    private LocalDateTime updatedAt;

    private List<FinanceAccountSubjectSummaryVO> children = new ArrayList<>();

    @JsonProperty("bd_c")
    private Integer bdC;
}
