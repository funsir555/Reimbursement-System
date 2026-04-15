// 业务域：流程模板与流程配置
// 文件角色：service 接口
// 上下游关系：上游通常来自 流程管理页面对应的 Controller，下游会继续协调 流程模板、报销类型、自定义档案和发布状态。
// 风险提醒：改坏后最容易影响 审批路由、模板发布和后续单据流转。

package com.finex.auth.service;

import com.finex.auth.dto.ProcessFlowDetailVO;
import com.finex.auth.dto.ProcessFlowMetaVO;
import com.finex.auth.dto.ProcessFlowResolveApproversDTO;
import com.finex.auth.dto.ProcessFlowResolveApproversVO;
import com.finex.auth.dto.ProcessFlowSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneVO;
import com.finex.auth.dto.ProcessFlowSummaryVO;
import com.finex.auth.dto.ProcessFormOptionVO;

import java.util.List;
import java.util.Map;

/**
 * ProcessFlowDesignService：service 接口。
 * 定义流程流程设计这块对外提供的业务入口能力。
 * 改这里时，要特别关注 审批路由、模板发布和后续单据流转是否会被一起带坏。
 */
public interface ProcessFlowDesignService {

    /**
     * 查询流程列表。
     */
    List<ProcessFlowSummaryVO> listFlows();

    /**
     * 获取流程元数据。
     */
    ProcessFlowMetaVO getFlowMeta();

    /**
     * 获取流程明细。
     */
    ProcessFlowDetailVO getFlowDetail(Long id);

    /**
     * 创建流程。
     */
    ProcessFlowDetailVO createFlow(ProcessFlowSaveDTO dto);

    /**
     * 更新流程。
     */
    ProcessFlowDetailVO updateFlow(Long id, ProcessFlowSaveDTO dto);

    /**
     * 发布流程。
     */
    ProcessFlowDetailVO publishFlow(Long id);

    /**
     * 更新流程Status。
     */
    Boolean updateFlowStatus(Long id, String status);

    /**
     * 创建流程Scene。
     */
    ProcessFlowSceneVO createFlowScene(ProcessFlowSceneSaveDTO dto);

    /**
     * 解析Approvers。
     */
    ProcessFlowResolveApproversVO resolveApprovers(ProcessFlowResolveApproversDTO dto);

    /**
     * 查询Published流程选项。
     */
    List<ProcessFormOptionVO> listPublishedFlowOptions();

    Map<String, String> publishedFlowLabelMap();
}
