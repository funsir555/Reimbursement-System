package com.finex.auth.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FinancePeriodTransferPreviewDetailVO {

    private Long detailId;

    private Integer lineNo;

    private String sourceSubjectCode;

    private String sourceSubjectName;

    private String sourceAssistLabel;

    private String targetSubjectCode;

    private String targetSubjectName;

    private String metricType;

    private String metricTypeLabel;

    private String direction;

    private String directionLabel;

    private BigDecimal amount;

    private String skipReason;

    private String sourceTraceKey;
}
