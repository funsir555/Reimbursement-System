package com.finex.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FinanceDepartmentVO {

    private Long id;

    private String deptCode;

    private String deptName;

    private Long parentId;

    private String parentDeptName;

    private String companyId;

    private String companyName;

    private Long leaderUserId;

    private String leaderName;

    private Integer status;

    private String syncSource;

    private Boolean syncManaged;

    private Boolean syncEnabled;

    private String syncStatus;

    private String syncRemark;

    private Integer sortOrder;

    private LocalDateTime lastSyncAt;

    private String statDepartmentBelong;

    private String statRegionBelong;

    private String statAreaBelong;
}
