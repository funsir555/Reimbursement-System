package com.finex.auth.dto;

import lombok.Data;

@Data
public class ProcessFlowResolvedUserVO {

    private Long userId;

    private String userName;

    private Long deptId;

    private String deptName;
}
