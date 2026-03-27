package com.finex.auth.dto;

import lombok.Data;

@Data
public class EmployeeQueryDTO {

    private String keyword;

    private String companyId;

    private Long deptId;

    private Integer status;
}
