package com.finex.auth.agent.workflow;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 受控工具执行结果。
 */
@Data
@Builder
public class ControlledToolResult {

    private boolean success;
    private String errorMessage;
    private String summary;
    private List<Map<String, Object>> records;
    private List<String> evidenceRefs;
}
