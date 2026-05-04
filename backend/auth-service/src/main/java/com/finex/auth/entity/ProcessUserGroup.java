package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_user_group")
public class ProcessUserGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String groupCode;

    private String groupName;

    private Integer codeLevel;

    private String codePrefix;

    private String memberUserIdsJson;

    private String scopeConditionGroupsJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
