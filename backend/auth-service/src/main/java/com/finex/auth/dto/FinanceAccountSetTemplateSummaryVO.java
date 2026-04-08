package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceAccountSetTemplateSummaryVO {

    private String templateCode;

    private String templateName;

    private String accountingStandard;

    private Integer level1SubjectCount;

    private Integer commonSubjectCount;
}
