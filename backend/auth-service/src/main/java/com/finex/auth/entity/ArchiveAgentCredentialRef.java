package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ea_agent_credential_ref")
public class ArchiveAgentCredentialRef {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ownerUserId;

    private String credentialCode;

    private String providerCode;

    private String credentialName;

    private String maskedKey;

    private String secretPayload;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
