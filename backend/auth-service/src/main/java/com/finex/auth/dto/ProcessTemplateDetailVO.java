package com.finex.auth.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProcessTemplateDetailVO {

    private Long id;

    private String templateCode;

    private String templateType;

    private String templateTypeLabel;

    private String templateName;

    private String templateDescription;

    private String category;

    private Boolean enabled;

    private String formDesign;

    private String printMode;

    private String approvalFlow;

    private String paymentMode;

    private String allocationForm;

    private String aiAuditMode;

    private List<String> scopeDeptIds;

    private List<String> scopeExpenseTypeCodes;

    private BigDecimal amountMin;

    private BigDecimal amountMax;

    private String tagOption;

    private String installmentOption;
}
