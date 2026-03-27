package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmployeeSaveDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "姓名不能为空")
    private String name;

    private String phone;

    private String email;

    private String companyId;

    private Long deptId;

    private String position;

    private String laborRelationBelong;

    private Integer status;
}
