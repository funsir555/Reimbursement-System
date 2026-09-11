package com.finex.auth.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class FinancePeriodTransferRuleVO {

    private Long id;

    private String companyId;

    private String ruleType;

    private String ruleTypeLabel;

    private String ruleName;

    private Boolean enabled;

    private String voucherType;

    private Boolean includeUnposted;

    private String transferGranularity;

    private Integer subjectLevel;

    private String sourceSubjectCode;

    private String sourceSubjectName;

    private String targetSubjectCode;

    private String targetSubjectName;

    private String allocationMode;

    private String regenerateStrategy;

    private String createdAt;

    private String updatedAt;

    private List<FinancePeriodTransferRuleLineVO> lines = new ArrayList<>();
}
