package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentSaveDTO {

    private String companyId;

    private String deptCode;

    private Long leaderUserId;

    @NotBlank(message = "\u90e8\u95e8\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a")
    private String deptName;

    private Long parentId;

    private String statDepartmentBelong;

    private String statRegionBelong;

    private String statAreaBelong;

    private Integer status;

    private Integer sortOrder;

    private Integer syncEnabled;
}
