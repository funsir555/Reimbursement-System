package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RolePermissionAssignDTO {

    private List<String> permissionCodes = new ArrayList<>();
}
