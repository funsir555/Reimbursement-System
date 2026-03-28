package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessFlowDetailVO {

    private Long id;

    private String flowCode;

    private String flowName;

    private String flowDescription;

    private String status;

    private String statusLabel;

    private Long editableVersionId;

    private Integer editableVersionNo;

    private Long publishedVersionId;

    private Integer publishedVersionNo;

    private Boolean hasDraftVersion;

    private List<ProcessFlowNodeDTO> nodes = new ArrayList<>();

    private List<ProcessFlowRouteDTO> routes = new ArrayList<>();
}
