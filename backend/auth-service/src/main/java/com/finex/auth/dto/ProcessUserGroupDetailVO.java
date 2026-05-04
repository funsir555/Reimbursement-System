package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessUserGroupDetailVO {

    private Long id;

    private Long parentId;

    private String groupCode;

    private String groupName;

    private Integer codeLevel;

    private List<String> memberUserIds = new ArrayList<>();

    private List<ProcessFlowConditionGroupDTO> scopeConditionGroups = new ArrayList<>();
}
