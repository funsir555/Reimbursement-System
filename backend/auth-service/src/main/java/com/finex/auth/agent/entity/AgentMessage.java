package com.finex.auth.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * Agent 消息实体
 */
@Data
@TableName("ea_agent_message")
public class AgentMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 业务公司主体编码。
     */
    private String companyId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 角色（user/assistant/system）
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 脱敏后内容
     */
    private String maskedContent;

    /**
     * 意图分类
     */
    private String intent;

    /**
     * Token 数量
     */
    private Integer tokens;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 追踪ID
     */
    private String traceId;
}
