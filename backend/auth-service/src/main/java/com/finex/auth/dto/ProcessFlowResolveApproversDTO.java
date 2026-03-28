package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ProcessFlowResolveApproversDTO {

    @NotNull(message = "流程ID不能为空")
    private Long flowId;

    @NotBlank(message = "节点标识不能为空")
    private String nodeKey;

    private Map<String, Object> context = new LinkedHashMap<>();
}
