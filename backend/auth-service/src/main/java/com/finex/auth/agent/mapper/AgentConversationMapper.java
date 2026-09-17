package com.finex.auth.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finex.auth.agent.entity.AgentConversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent 会话 Mapper
 */
@Mapper
public interface AgentConversationMapper extends BaseMapper<AgentConversation> {
}
