package com.finex.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessFlowSaveDTO {

    @NotBlank(message = "\u6d41\u7a0b\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a")
    @Size(max = 64, message = "\u6d41\u7a0b\u540d\u79f0\u6700\u591a 64 \u4e2a\u5b57\u7b26")
    private String flowName;

    private String flowDescription;

    @Valid
    private List<ProcessFlowNodeDTO> nodes = new ArrayList<>();

    @Valid
    private List<ProcessFlowRouteDTO> routes = new ArrayList<>();
}
