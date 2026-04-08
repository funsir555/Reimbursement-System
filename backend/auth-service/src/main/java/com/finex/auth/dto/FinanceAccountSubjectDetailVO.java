package com.finex.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FinanceAccountSubjectDetailVO {

    private Long id;

    private String companyId;

    private String subjectCode;

    private String subjectName;

    private String parentSubjectCode;

    private Integer subjectLevel;

    private String balanceDirection;

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

    private Integer bcDefine1;

    private Integer bcDefine2;

    private Integer bcDefine3;

    private Integer bcDefine4;

    private Integer bcDefine5;

    private Integer bcDefine6;

    private Integer bcDefine7;

    private Integer bcDefine8;

    private Integer bcDefine9;

    private Integer bcDefine10;

    private Integer bcDefine11;

    private Integer bcDefine12;

    private Integer bcDefine13;

    private Integer bcDefine14;

    private Integer bcDefine15;

    private Integer bcDefine16;

    private Integer status;

    private String templateCode;

    private Integer sortOrder;

    private Boolean hasChildren;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
