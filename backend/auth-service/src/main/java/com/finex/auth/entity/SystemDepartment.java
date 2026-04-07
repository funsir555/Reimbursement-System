package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_department")
public class SystemDepartment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;

    private String deptCode;

    private Long leaderUserId;

    private String deptName;

    private Long parentId;

    private String wecomDepartmentId;

    private String dingtalkDepartmentId;

    private String feishuDepartmentId;

    private String syncSource;

    private Integer syncEnabled;

    private Integer syncManaged;

    private String syncStatus;

    private String syncRemark;

    private String statDepartmentBelong;

    private String statRegionBelong;

    private String statAreaBelong;

    private LocalDateTime lastSyncAt;

    private Integer status;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
