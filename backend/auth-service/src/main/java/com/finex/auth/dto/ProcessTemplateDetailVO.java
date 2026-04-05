package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
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

    private String expenseDetailDesign;

    private String expenseDetailType;

    private String expenseDetailModeDefault;

    private String printMode;

    private String approvalFlow;

    private String paymentMode;

    private String allocationForm;

    private String aiAuditMode;

    private List<String> scopeDeptIds;

    private List<String> scopeExpenseTypeCodes;

    @MoneyValue
    private BigDecimal amountMin;

    @MoneyValue
    private BigDecimal amountMax;

    private String tagOption;

    private String installmentOption;
}
