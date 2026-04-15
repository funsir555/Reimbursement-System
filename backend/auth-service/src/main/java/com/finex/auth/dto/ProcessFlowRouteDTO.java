package com.finex.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessFlowRouteDTO {

    private String routeKey;
    private String sourceNodeKey;
    private String targetNodeKey;

    @Size(max = 64, message = "\u5206\u652f\u540d\u79f0\u6700\u591a 64 \u4e2a\u5b57\u7b26")
    private String routeName;

    private Integer priority;
    private Boolean defaultRoute;

    @Valid
    private List<ProcessFlowConditionGroupDTO> conditionGroups = new ArrayList<>();
}
