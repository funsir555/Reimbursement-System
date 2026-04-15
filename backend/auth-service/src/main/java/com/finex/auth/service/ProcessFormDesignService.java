// 业务域：流程模板与流程配置
// 文件角色：service 接口
// 上下游关系：上游通常来自 流程管理页面对应的 Controller，下游会继续协调 流程模板、报销类型、自定义档案和发布状态。
// 风险提醒：改坏后最容易影响 审批路由、模板发布和后续单据流转。

package com.finex.auth.service;

import com.finex.auth.dto.ProcessFormDesignDetailVO;
import com.finex.auth.dto.ProcessFormDesignSaveDTO;
import com.finex.auth.dto.ProcessFormDesignSummaryVO;
import com.finex.auth.dto.ProcessFormOptionVO;

import java.util.List;
import java.util.Map;

/**
 * ProcessFormDesignService：service 接口。
 * 定义流程表单设计这块对外提供的业务入口能力。
 * 改这里时，要特别关注 审批路由、模板发布和后续单据流转是否会被一起带坏。
 */
public interface ProcessFormDesignService {

    /**
     * 查询表单设计列表。
     */
    List<ProcessFormDesignSummaryVO> listFormDesigns(String templateType);

    /**
     * 获取表单设计明细。
     */
    ProcessFormDesignDetailVO getFormDesignDetail(Long id);

    /**
     * 创建表单设计。
     */
    ProcessFormDesignDetailVO createFormDesign(ProcessFormDesignSaveDTO dto);

    /**
     * 更新表单设计。
     */
    ProcessFormDesignDetailVO updateFormDesign(Long id, ProcessFormDesignSaveDTO dto);

    /**
     * 删除表单设计。
     */
    Boolean deleteFormDesign(Long id);

    /**
     * 查询表单设计选项。
     */
    List<ProcessFormOptionVO> listFormDesignOptions(String templateType);

    Map<String, String> formDesignLabelMap(String templateType);

    /**
     * 解析表单设计编码。
     */
    String resolveFormDesignCode(String formCode, String templateType);
}
