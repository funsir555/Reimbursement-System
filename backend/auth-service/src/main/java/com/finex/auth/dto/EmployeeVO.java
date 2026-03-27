package com.finex.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class EmployeeVO {

    private Long userId;

    private String username;

    private String name;

    private String phone;

    private String email;

    private String companyId;

    private String companyName;

    private Long deptId;

    private String deptName;

    private String position;

    private String laborRelationBelong;

    private Integer status;

    private String sourceType;

    private Boolean syncManaged;

    private LocalDateTime lastSyncAt;

    private List<String> roleCodes = new ArrayList<>();
}
