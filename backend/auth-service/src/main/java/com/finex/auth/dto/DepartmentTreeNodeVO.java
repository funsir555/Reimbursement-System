package com.finex.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DepartmentTreeNodeVO {

    private Long id;

    private String companyId;

    private String deptCode;

    private Long leaderUserId;

    private String leaderName;

    private String deptName;

    private Long parentId;

    private String syncSource;

    private Boolean syncManaged;

    private Boolean syncEnabled;

    private String syncStatus;

    private String syncRemark;

    private String statDepartmentBelong;

    private String statRegionBelong;

    private String statAreaBelong;

    private Integer status;

    private Integer sortOrder;

    private LocalDateTime lastSyncAt;

    private List<DepartmentTreeNodeVO> children = new ArrayList<>();
}
