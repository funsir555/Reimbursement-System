// 业务域：流程模板与流程配置
// 文件角色：service 接口
// 上下游关系：上游通常来自 流程管理页面对应的 Controller，下游会继续协调 流程模板、报销类型、自定义档案和发布状态。
// 风险提醒：改坏后最容易影响 审批路由、模板发布和后续单据流转。

package com.finex.auth.service;

import com.finex.auth.dto.ProcessExpenseDetailDesignDetailVO;
import com.finex.auth.dto.ProcessExpenseDetailDesignSaveDTO;
import com.finex.auth.dto.ProcessExpenseDetailDesignSummaryVO;

import java.util.List;
import java.util.Map;

/**
 * ProcessExpenseDetailDesignService：service 接口。
 * 定义流程报销单明细设计这块对外提供的业务入口能力。
 * 改这里时，要特别关注 审批路由、模板发布和后续单据流转是否会被一起带坏。
 */
public interface ProcessExpenseDetailDesignService {

    /**
     * 查询报销单明细设计列表。
     */
    List<ProcessExpenseDetailDesignSummaryVO> listExpenseDetailDesigns();

    /**
     * 获取报销单明细设计明细。
     */
    ProcessExpenseDetailDesignDetailVO getExpenseDetailDesignDetail(Long id);

    /**
     * 创建报销单明细设计。
     */
    ProcessExpenseDetailDesignDetailVO createExpenseDetailDesign(ProcessExpenseDetailDesignSaveDTO dto);

    /**
     * 更新报销单明细设计。
     */
    ProcessExpenseDetailDesignDetailVO updateExpenseDetailDesign(Long id, ProcessExpenseDetailDesignSaveDTO dto);

    /**
     * 删除报销单明细设计。
     */
    Boolean deleteExpenseDetailDesign(Long id);

    Map<String, String> detailDesignLabelMap();

    /**
     * 解析报销单明细设计编码。
     */
    String resolveExpenseDetailDesignCode(String detailCode);

    /**
     * 解析报销单明细类型。
     */
    String resolveExpenseDetailType(String detailCode);
}
