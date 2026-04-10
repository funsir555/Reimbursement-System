package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceAccountSetSummaryVO {

    private String companyId;

    private String companyCode;

    private String companyName;

    private String status;

    private String statusLabel;

    private String enabledYearMonth;

    private String templateCode;

    private String templateName;

    private Long supervisorUserId;

    private String supervisorName;

    private String createMode;

    private String referenceCompanyId;

    private String referenceCompanyName;

    private String subjectCodeScheme;

    private Integer subjectCount;

    private String lastTaskNo;

    private String lastTaskStatus;

    private Integer lastTaskProgress;

    private String lastTaskMessage;

    private String updatedAt;
}
