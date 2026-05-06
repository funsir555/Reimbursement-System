package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExpenseActionUserOptionVO {

    private Long userId;

    private String name;

    private String username;

    private Long deptId;

    private String deptName;

    private List<EmployeeDepartmentRefVO> departments = new ArrayList<>();

    private String phone;

    private String email;

    private Integer status;
}
