package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessFlowMetaVO {

    private List<ProcessFormOptionVO> nodeTypeOptions = new ArrayList<>();

    private List<ProcessFlowSceneVO> sceneOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> approvalApproverTypeOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> approvalManagerRuleModeOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> approvalManagerDeptSourceOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> approvalManagerLevelOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> approvalManagerLookupLevelOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> approvalManualCandidateScopeOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> ccReceiverTypeOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> paymentExecutorTypeOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> missingHandlerOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> approvalModeOptions = new ArrayList<>();

    private List<String> defaultApprovalOpinions = new ArrayList<>();

    private List<ProcessFlowConfigOptionVO> approvalSpecialOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> ccTimingOptions = new ArrayList<>();

    private List<ProcessFlowConfigOptionVO> ccSpecialOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> paymentActionOptions = new ArrayList<>();

    private List<ProcessFlowConfigOptionVO> paymentSpecialOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> branchOperatorOptions = new ArrayList<>();

    private List<ProcessFlowConditionFieldVO> branchConditionFields = new ArrayList<>();

    private List<ProcessFormOptionVO> departmentOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> userOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> expenseTypeOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> archiveOptions = new ArrayList<>();
}
