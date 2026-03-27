package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String name;

    private String phone;

    private String email;

    private String companyId;

    private Long deptId;

    private String position;

    private String laborRelationBelong;

    private Integer status;

    private String sourceType;

    private Integer syncManaged;

    private String wecomUserId;

    private String dingtalkUserId;

    private String feishuUserId;

    private LocalDateTime lastSyncAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
