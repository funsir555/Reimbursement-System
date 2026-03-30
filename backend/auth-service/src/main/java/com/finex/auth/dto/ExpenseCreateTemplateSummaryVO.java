package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseCreateTemplateSummaryVO {

    private String templateCode;
    private String templateName;
    private String templateType;
    private String templateTypeLabel;
    private String categoryCode;
    private String formDesignCode;
}
