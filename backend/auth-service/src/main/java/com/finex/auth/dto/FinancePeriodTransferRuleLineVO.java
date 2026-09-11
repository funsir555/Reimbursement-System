package com.finex.auth.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FinancePeriodTransferRuleLineVO {

    private Long id;

    private Integer lineNo;

    private String sourceSubjectCode;

    private String sourceSubjectName;

    private String targetSubjectCode;

    private String targetSubjectName;

    private String metricType;

    private String metricTypeLabel;

    private BigDecimal ratio;

    private String allocationProjectClass;

    private String allocationProjectId;

    private String allocationProjectName;

    private String remark;
}
