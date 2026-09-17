package com.finex.auth.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finex.auth.agent.entity.AgentMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent 消息 Mapper
 */
@Mapper
public interface AgentMessageMapper extends BaseMapper<AgentMessage> {
}
