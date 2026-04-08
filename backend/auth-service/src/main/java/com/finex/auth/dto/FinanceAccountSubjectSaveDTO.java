package com.finex.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FinanceAccountSubjectSaveDTO {

    @NotBlank(message = "科目编码不能为空")
    private String subjectCode;

    @NotBlank(message = "科目名称不能为空")
    private String subjectName;

    private String parentSubjectCode;

    private Integer subjectLevel;

    private String subjectCategory;

    private String cclassany;

    private Integer bproperty;

    private String cbookType;

    private String chelp;

    private String cexchName;

    private String cmeasure;

    private Integer bperson;

    private Integer bcus;

    private Integer bsup;

    private Integer bdept;

    private Integer bitem;

    private String cassItem;

    private Integer br;

    private Integer be;

    private String cgather;

    private Integer leafFlag;

    private Integer bexchange;

    private Integer bcash;

    private Integer bbank;

    private Integer bused;

    @JsonProperty("bd_c")
    private Integer bdC;

    private LocalDateTime dbegin;

    private LocalDateTime dend;

    private Integer itrans;

    private Integer bclose;

    private String cother;

    private Integer iotherused;

    @JsonProperty("bReport")
    private Integer bReport;

    @JsonProperty("bGCJS")
    private Integer bGCJS;

    @JsonProperty("bCashItem")
    private Integer bCashItem;

    @JsonProperty("iViewItem")
    private Integer iViewItem;
}
