package com.finex.auth.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FinancePeriodTransferRunVO {

    private Long id;

    private Long ruleId;

    private String ruleType;

    private String ruleTypeLabel;

    private String ruleName;

    private Integer iyear;

    private Integer iperiod;

    private Integer iyperiod;

    private String status;

    private String statusLabel;

    private String previewToken;

    private Boolean includeUnposted;

    private String voucherType;

    private String voucherNo;

    private Integer generatedEntryCount;

    private Integer skippedEntryCount;

    private BigDecimal totalAmount;

    private String blockedMessage;

    private String previewedBy;

    private String previewedAt;

    private String generatedBy;

    private String generatedAt;
}
