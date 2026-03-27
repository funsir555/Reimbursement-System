package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 保存模板请求
 */
@Data
public class ProcessTemplateSaveDTO {

    @NotBlank(message = "模板类型不能为空")
    private String templateType;

    @NotBlank(message = "单据名称不能为空")
    private String templateName;

    private String templateDescription;

    private String category;

    private String iconColor;

    private Boolean enabled;

    private String printMode;

    private String approvalFlow;

    private String paymentMode;

    private String allocationForm;

    private List<String> expenseTypes;

    private String aiAuditMode;

    private List<String> scopeOptions;

    private String tagOption;

    private String installmentOption;
}
