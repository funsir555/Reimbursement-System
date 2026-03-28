package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessFlowRouteDTO {

    private String routeKey;

    private String sourceNodeKey;

    private String targetNodeKey;

    private String routeName;

    private Integer priority;

    private Boolean defaultRoute;

    private List<ProcessFlowConditionGroupDTO> conditionGroups = new ArrayList<>();
}
