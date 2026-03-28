package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessFlowSaveDTO {

    @NotBlank(message = "流程名称不能为空")
    private String flowName;

    private String flowDescription;

    private List<ProcessFlowNodeDTO> nodes = new ArrayList<>();

    private List<ProcessFlowRouteDTO> routes = new ArrayList<>();
}
