package com.finex.auth.dto;

import lombok.Data;

import java.util.List;

/**
 * 新建模板页面选项
 */
@Data
public class ProcessTemplateFormOptionsVO {

    private String templateType;

    private String templateTypeLabel;

    private List<ProcessFormOptionVO> categoryOptions;

    private List<ProcessFormOptionVO> numberingRules;

    private List<ProcessFormOptionVO> printModes;

    private List<ProcessFormOptionVO> approvalFlows;

    private List<ProcessFormOptionVO> paymentModes;

    private List<ProcessFormOptionVO> travelForms;

    private List<ProcessFormOptionVO> allocationForms;

    private List<ProcessFormOptionVO> expenseTypes;

    private List<ProcessFormOptionVO> aiAuditModes;

    private List<ProcessFormOptionVO> scopeOptions;

    private List<ProcessFormOptionVO> tagOptions;
}
