package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessFlowResolveApproversVO {

    private String resolutionType;

    private String nextAction;

    private List<Long> approverUserIds = new ArrayList<>();

    private List<ProcessFlowResolvedUserVO> approverUsers = new ArrayList<>();

    private List<String> trace = new ArrayList<>();
}
