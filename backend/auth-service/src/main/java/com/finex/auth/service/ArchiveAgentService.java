// 业务域：档案代理与归档任务
// 文件角色：service 接口
// 上下游关系：上游通常来自 档案代理配置接口和后台调度，下游会继续协调 归档规则、执行记录和调度计划。
// 风险提醒：改坏后最容易影响 档案归集效果、执行漏掉和后续追溯。

package com.finex.auth.service;

import com.finex.auth.dto.ArchiveAgentDetailVO;
import com.finex.auth.dto.ArchiveAgentMetaVO;
import com.finex.auth.dto.ArchiveAgentRunDTO;
import com.finex.auth.dto.ArchiveAgentRunDetailVO;
import com.finex.auth.dto.ArchiveAgentRunVO;
import com.finex.auth.dto.ArchiveAgentSaveDTO;
import com.finex.auth.dto.ArchiveAgentSummaryVO;

import java.util.List;

/**
 * ArchiveAgentService：service 接口。
 * 定义档案代理这块对外提供的业务入口能力。
 * 改这里时，要特别关注 档案归集效果、执行漏掉和后续追溯是否会被一起带坏。
 */
public interface ArchiveAgentService {

    /**
     * 查询代理列表。
     */
    List<ArchiveAgentSummaryVO> listAgents(Long ownerUserId, String keyword, String status);

    /**
     * 获取元数据。
     */
    ArchiveAgentMetaVO getMeta();

    /**
     * 获取代理明细。
     */
    ArchiveAgentDetailVO getAgentDetail(Long ownerUserId, Long id);

    /**
     * 创建代理。
     */
    ArchiveAgentDetailVO createAgent(Long ownerUserId, String operatorName, ArchiveAgentSaveDTO dto);

    /**
     * 更新代理。
     */
    ArchiveAgentDetailVO updateAgent(Long ownerUserId, Long id, String operatorName, ArchiveAgentSaveDTO dto);

    /**
     * 发布代理。
     */
    ArchiveAgentDetailVO publishAgent(Long ownerUserId, Long id, String operatorName);

    /**
     * 更新代理Status。
     */
    ArchiveAgentDetailVO updateAgentStatus(Long ownerUserId, Long id, String status);

    /**
     * 执行代理。
     */
    ArchiveAgentRunVO runAgent(Long ownerUserId, Long id, ArchiveAgentRunDTO dto);

    /**
     * 查询执行列表。
     */
    List<ArchiveAgentRunVO> listRuns(Long ownerUserId, Long agentId);

    /**
     * 获取执行明细。
     */
    ArchiveAgentRunDetailVO getRunDetail(Long ownerUserId, Long runId);

    /**
     * 执行Due调度。
     */
    void runDueSchedules();
}
