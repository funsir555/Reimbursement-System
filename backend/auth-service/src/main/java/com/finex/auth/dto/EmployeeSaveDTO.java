package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmployeeSaveDTO {

    @NotBlank(message = "\u7528\u6237\u540d\u4e0d\u80fd\u4e3a\u7a7a")
    private String username;

    @NotBlank(message = "\u59d3\u540d\u4e0d\u80fd\u4e3a\u7a7a")
    private String name;

    private String phone;

    private String email;

    private String companyId;

    private Long deptId;

    private String position;

    private String laborRelationBelong;

    private String statDepartmentBelong;

    private String statRegionBelong;

    private String statAreaBelong;

    private Integer status;
}
