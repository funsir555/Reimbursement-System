package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PermissionTreeNodeVO {

    private Long id;

    private String permissionCode;

    private String permissionName;

    private String permissionType;

    private Long parentId;

    private String moduleCode;

    private String routePath;

    private Integer sortOrder;

    private List<PermissionTreeNodeVO> children = new ArrayList<>();
}
