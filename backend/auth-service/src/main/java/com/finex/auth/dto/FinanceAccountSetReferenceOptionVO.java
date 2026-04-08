package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceAccountSetReferenceOptionVO {

    private String companyId;

    private String companyName;

    private String templateCode;

    private String templateName;

    private String enabledYearMonth;

    private String subjectCodeScheme;

    private String label;
}
