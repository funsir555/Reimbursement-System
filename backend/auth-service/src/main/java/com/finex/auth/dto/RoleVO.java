package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RoleVO {

    private Long id;

    private String roleCode;

    private String roleName;

    private String roleDescription;

    private Integer status;

    private List<String> permissionCodes = new ArrayList<>();

    private List<Long> userIds = new ArrayList<>();

    private List<String> userNames = new ArrayList<>();
}
