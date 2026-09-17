package com.finex.auth.agent.workflow;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Workflow Selector 的选择结果。
 */
@Data
@Builder
public class SelectedWorkflow {

    private Long agentId;
    private Long agentVersionId;
    private Long ownerUserId;
    private String agentName;
    private String toolCode;
    private String providerCode;
    private String model;
    private Map<String, Object> config;
    private boolean builtInFallback;
}
