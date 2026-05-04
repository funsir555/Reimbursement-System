package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessUserGroupTreeVO {

    private Long id;

    private Long parentId;

    private String groupCode;

    private String groupName;

    private Integer codeLevel;

    private List<ProcessUserGroupTreeVO> children = new ArrayList<>();
}
