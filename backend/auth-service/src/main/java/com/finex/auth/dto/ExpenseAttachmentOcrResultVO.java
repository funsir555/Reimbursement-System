package com.finex.auth.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExpenseAttachmentOcrResultVO {

    private String status;

    private String providerCode;

    private String providerName;

    private String requestId;

    private LocalDateTime recognizedAt;

    private String invoiceCode;

    private String invoiceNumber;

    private String invoiceDate;

    private String invoiceType;

    private String sellerName;

    private BigDecimal totalAmount;

    private BigDecimal taxAmount;

    private String message;
}
