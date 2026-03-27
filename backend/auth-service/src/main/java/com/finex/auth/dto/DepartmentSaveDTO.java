package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentSaveDTO {

    private String companyId;

    private String deptCode;

    private Long leaderUserId;

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    private Long parentId;

    private Integer status;

    private Integer sortOrder;

    private Integer syncEnabled;
}
