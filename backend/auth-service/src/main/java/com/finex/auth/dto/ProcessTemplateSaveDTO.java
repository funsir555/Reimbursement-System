package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProcessTemplateSaveDTO {

    @NotBlank(message = "\u6a21\u677f\u7c7b\u578b\u4e0d\u80fd\u4e3a\u7a7a")
    private String templateType;

    @NotBlank(message = "\u5355\u636e\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a")
    @Size(max = 64, message = "\u5355\u636e\u540d\u79f0\u6700\u591a 64 \u4e2a\u5b57\u7b26")
    private String templateName;

    private String templateDescription;
    private String category;
    private Boolean enabled;
    private String formDesign;
    private String expenseDetailDesign;
    private String expenseDetailModeDefault;
    private String printMode;
    private String approvalFlow;
    private String paymentMode;
    private String allocationForm;
    private String aiAuditMode;
    private List<String> scopeDeptIds;
    private List<String> scopeExpenseTypeCodes;

    @MoneyInput
    private BigDecimal amountMin;

    @MoneyInput
    private BigDecimal amountMax;

    private String tagOption;
    private String installmentOption;
}
