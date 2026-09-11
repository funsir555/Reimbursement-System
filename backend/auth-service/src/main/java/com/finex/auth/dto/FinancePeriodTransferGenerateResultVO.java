package com.finex.auth.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FinancePeriodTransferGenerateResultVO {

    private Long runId;

    private String voucherNo;

    private String displayVoucherNo;

    private Integer generatedEntryCount;

    private Integer skippedEntryCount;

    private BigDecimal totalAmount;

    private String message;
}
