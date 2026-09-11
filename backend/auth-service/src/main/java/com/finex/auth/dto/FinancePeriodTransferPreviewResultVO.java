package com.finex.auth.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class FinancePeriodTransferPreviewResultVO {

    private Long runId;

    private Long ruleId;

    private String ruleType;

    private String ruleTypeLabel;

    private String ruleName;

    private String previewToken;

    private String voucherType;

    private Integer generatedEntryCount;

    private Integer skippedEntryCount;

    private BigDecimal totalAmount;

    private String message;

    private List<FinancePeriodTransferPreviewDetailVO> details = new ArrayList<>();
}
