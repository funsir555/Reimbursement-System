package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceAccountSetTaskPayload {

    private String createMode;

    private String referenceCompanyId;

    private String targetCompanyId;

    private String enabledYearMonth;

    private String templateCode;

    private Long supervisorUserId;

    private String subjectCodeScheme;
}
