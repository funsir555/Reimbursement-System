package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseVoucherTemplatePolicyVO {

    private Long id;
    private String companyId;
    private String companyName;
    private String templateCode;
    private String templateName;
    private String creditAccountCode;
    private String creditAccountName;
    private String personRule;
    private String supplierRule;
    private String deptRule;
    private String projectRule;
    private String voucherType;
    private String voucherTypeLabel;
    private String summaryRule;
    private Boolean enabled;
    private String updatedAt;
}
