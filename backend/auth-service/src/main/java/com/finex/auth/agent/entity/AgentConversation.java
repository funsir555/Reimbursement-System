package com.finex.auth.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * Agent 会话实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ea_agent_conversation")
public class AgentConversation extends TenantAwareEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID（唯一标识）
     */
    private String sessionId;

    private String customTitle;

    private Boolean isPinned;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 上下文数据（JSON）
     */
    private String contextJson;

    /**
     * 会话摘要
     */
    private String summaryText;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 最后消息时间
     */
    private Instant lastMessageAt;

    /**
     * 过期时间
     */
    private Instant expiresAt;
}
