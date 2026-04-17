// 业务域：流程模板与流程配置
// 文件角色：service 接口
// 上下游关系：上游通常来自 流程管理页面对应的 Controller，下游会继续协调 流程模板、报销类型、自定义档案和发布状态。
// 风险提醒：改坏后最容易影响 审批路由、模板发布和后续单据流转。

package com.finex.auth.service;

import com.finex.auth.dto.ProcessCenterOverviewVO;
import com.finex.auth.dto.ProcessCustomArchiveDetailVO;
import com.finex.auth.dto.ProcessCustomArchiveMetaVO;
import com.finex.auth.dto.ProcessCustomArchiveResolveDTO;
import com.finex.auth.dto.ProcessCustomArchiveResolveResultVO;
import com.finex.auth.dto.ProcessCustomArchiveSaveDTO;
import com.finex.auth.dto.ProcessCustomArchiveSummaryVO;
import com.finex.auth.dto.ProcessExpenseTypeDetailVO;
import com.finex.auth.dto.ProcessExpenseTypeMetaVO;
import com.finex.auth.dto.ProcessExpenseTypeSaveDTO;
import com.finex.auth.dto.ProcessExpenseTypeTreeVO;
import com.finex.auth.dto.ProcessExpenseDetailDesignDetailVO;
import com.finex.auth.dto.ProcessExpenseDetailDesignSaveDTO;
import com.finex.auth.dto.ProcessExpenseDetailDesignSummaryVO;
import com.finex.auth.dto.ProcessFormDesignDetailVO;
import com.finex.auth.dto.ProcessFormDesignSaveDTO;
import com.finex.auth.dto.ProcessFormDesignSummaryVO;
import com.finex.auth.dto.ProcessFlowDetailVO;
import com.finex.auth.dto.ProcessFlowMetaVO;
import com.finex.auth.dto.ProcessFlowResolveApproversDTO;
import com.finex.auth.dto.ProcessFlowResolveApproversVO;
import com.finex.auth.dto.ProcessFlowSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneVO;
import com.finex.auth.dto.ProcessFlowSummaryVO;
import com.finex.auth.dto.ProcessTemplateDetailVO;
import com.finex.auth.dto.ProcessTemplateFormOptionsVO;
import com.finex.auth.dto.ProcessTemplateSaveDTO;
import com.finex.auth.dto.ProcessTemplateSaveResultVO;
import com.finex.auth.dto.ProcessTemplateTypeVO;

import java.util.List;

/**
 * ProcessManagementService：service 接口。
 * 定义流程管理这块对外提供的业务入口能力。
 * 改这里时，要特别关注 审批路由、模板发布和后续单据流转是否会被一起带坏。
 */
public interface ProcessManagementService {

    /**
     * 获取Overview。
     */
    ProcessCenterOverviewVO getOverview();

    /**
     * 获取模板类型。
     */
    List<ProcessTemplateTypeVO> getTemplateTypes();

    /**
     * 获取表单选项。
     */
    ProcessTemplateFormOptionsVO getFormOptions(String templateType);

    /**
     * 获取模板明细。
     */
    ProcessTemplateDetailVO getTemplateDetail(Long id);

    /**
     * 保存模板。
     */
    ProcessTemplateSaveResultVO saveTemplate(ProcessTemplateSaveDTO dto, String operatorName);

    /**
     * 更新模板。
     */
    ProcessTemplateSaveResultVO updateTemplate(Long id, ProcessTemplateSaveDTO dto, String operatorName);

    ProcessTemplateSaveResultVO copyTemplate(Long id, String operatorName);

    /**
     * 删除模板。
     */
    Boolean deleteTemplate(Long id);

    /**
     * 查询自定义档案列表。
     */
    List<ProcessCustomArchiveSummaryVO> listCustomArchives();

    /**
     * 获取自定义档案明细。
     */
    ProcessCustomArchiveDetailVO getCustomArchiveDetail(Long id);

    /**
     * 创建自定义档案。
     */
    ProcessCustomArchiveDetailVO createCustomArchive(ProcessCustomArchiveSaveDTO dto);

    /**
     * 更新自定义档案。
     */
    ProcessCustomArchiveDetailVO updateCustomArchive(Long id, ProcessCustomArchiveSaveDTO dto);

    /**
     * 更新自定义档案Status。
     */
    Boolean updateCustomArchiveStatus(Long id, Integer status);

    /**
     * 删除自定义档案。
     */
    Boolean deleteCustomArchive(Long id);

    /**
     * 获取自定义档案元数据。
     */
    ProcessCustomArchiveMetaVO getCustomArchiveMeta();

    /**
     * 解析自定义档案。
     */
    ProcessCustomArchiveResolveResultVO resolveCustomArchive(ProcessCustomArchiveResolveDTO dto);

    /**
     * 查询报销单类型Tree列表。
     */
    List<ProcessExpenseTypeTreeVO> listExpenseTypeTree();

    /**
     * 获取报销单类型元数据。
     */
    ProcessExpenseTypeMetaVO getExpenseTypeMeta();

    /**
     * 获取报销单类型明细。
     */
    ProcessExpenseTypeDetailVO getExpenseTypeDetail(Long id);

    /**
     * 创建报销单类型。
     */
    ProcessExpenseTypeDetailVO createExpenseType(ProcessExpenseTypeSaveDTO dto);

    /**
     * 更新报销单类型。
     */
    ProcessExpenseTypeDetailVO updateExpenseType(Long id, ProcessExpenseTypeSaveDTO dto);

    /**
     * 更新报销单类型Status。
     */
    Boolean updateExpenseTypeStatus(Long id, Integer status);

    /**
     * 删除报销单类型。
     */
    Boolean deleteExpenseType(Long id);

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
     * 解析流程Approvers。
     */
    ProcessFlowResolveApproversVO resolveFlowApprovers(ProcessFlowResolveApproversDTO dto);
}
