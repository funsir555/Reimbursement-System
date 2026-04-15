// 业务域：流程模板与流程配置
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 流程管理页面对应的 Controller，下游会继续协调 流程模板、报销类型、自定义档案和发布状态。
// 风险提醒：改坏后最容易影响 审批路由、模板发布和后续单据流转。

package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessCenterOverviewVO;
import com.finex.auth.dto.ProcessCustomArchiveDetailVO;
import com.finex.auth.dto.ProcessCustomArchiveMetaVO;
import com.finex.auth.dto.ProcessCustomArchiveResolveDTO;
import com.finex.auth.dto.ProcessCustomArchiveResolveResultVO;
import com.finex.auth.dto.ProcessCustomArchiveSaveDTO;
import com.finex.auth.dto.ProcessCustomArchiveSummaryVO;
import com.finex.auth.dto.ProcessExpenseDetailDesignDetailVO;
import com.finex.auth.dto.ProcessExpenseDetailDesignSaveDTO;
import com.finex.auth.dto.ProcessExpenseDetailDesignSummaryVO;
import com.finex.auth.dto.ProcessExpenseTypeDetailVO;
import com.finex.auth.dto.ProcessExpenseTypeMetaVO;
import com.finex.auth.dto.ProcessExpenseTypeSaveDTO;
import com.finex.auth.dto.ProcessExpenseTypeTreeVO;
import com.finex.auth.dto.ProcessFlowDetailVO;
import com.finex.auth.dto.ProcessFlowMetaVO;
import com.finex.auth.dto.ProcessFlowResolveApproversDTO;
import com.finex.auth.dto.ProcessFlowResolveApproversVO;
import com.finex.auth.dto.ProcessFlowSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneVO;
import com.finex.auth.dto.ProcessFlowSummaryVO;
import com.finex.auth.dto.ProcessFormDesignDetailVO;
import com.finex.auth.dto.ProcessFormDesignSaveDTO;
import com.finex.auth.dto.ProcessFormDesignSummaryVO;
import com.finex.auth.dto.ProcessTemplateDetailVO;
import com.finex.auth.dto.ProcessTemplateFormOptionsVO;
import com.finex.auth.dto.ProcessTemplateSaveDTO;
import com.finex.auth.dto.ProcessTemplateSaveResultVO;
import com.finex.auth.dto.ProcessTemplateTypeVO;
import com.finex.auth.mapper.CodeSequenceMapper;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessCustomArchiveItemMapper;
import com.finex.auth.mapper.ProcessCustomArchiveRuleMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessTemplateCategoryMapper;
import com.finex.auth.mapper.ProcessTemplateScopeMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.ProcessExpenseDetailDesignService;
import com.finex.auth.service.ProcessFlowDesignService;
import com.finex.auth.service.ProcessFormDesignService;
import com.finex.auth.service.ProcessManagementService;
import com.finex.auth.service.impl.process.ProcessCenterDomainSupport;
import com.finex.auth.service.impl.process.ProcessCustomArchiveDomainSupport;
import com.finex.auth.service.impl.process.ProcessExpenseTypeDomainSupport;
import com.finex.auth.service.impl.process.ProcessTemplateDomainSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ProcessManagementServiceImpl：service 入口实现。
 * 接住上层请求，并把 流程管理相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 审批路由、模板发布和后续单据流转是否会被一起带坏。
 */
@Service
public class ProcessManagementServiceImpl implements ProcessManagementService {

    private final ProcessCenterDomainSupport processCenterDomainSupport;
    private final ProcessTemplateDomainSupport processTemplateDomainSupport;
    private final ProcessCustomArchiveDomainSupport processCustomArchiveDomainSupport;
    private final ProcessExpenseTypeDomainSupport processExpenseTypeDomainSupport;
    private final ProcessFormDesignService processFormDesignService;
    private final ProcessExpenseDetailDesignService processExpenseDetailDesignService;
    private final ProcessFlowDesignService processFlowDesignService;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public ProcessManagementServiceImpl(
            ProcessTemplateCategoryMapper categoryMapper,
            ProcessDocumentTemplateMapper templateMapper,
            CodeSequenceMapper codeSequenceMapper,
            ProcessTemplateScopeMapper scopeMapper,
            ProcessCustomArchiveDesignMapper customArchiveDesignMapper,
            ProcessCustomArchiveItemMapper customArchiveItemMapper,
            ProcessCustomArchiveRuleMapper customArchiveRuleMapper,
            ProcessExpenseTypeMapper processExpenseTypeMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper,
            ProcessFormDesignService processFormDesignService,
            ProcessExpenseDetailDesignService processExpenseDetailDesignService,
            ProcessFlowDesignService processFlowDesignService,
            ObjectMapper objectMapper
    ) {
        this.processCenterDomainSupport = new ProcessCenterDomainSupport(
                categoryMapper,
                templateMapper,
                codeSequenceMapper,
                scopeMapper,
                customArchiveDesignMapper,
                customArchiveItemMapper,
                customArchiveRuleMapper,
                processExpenseTypeMapper,
                systemDepartmentMapper,
                userMapper,
                processFormDesignService,
                processExpenseDetailDesignService,
                processFlowDesignService,
                objectMapper
        );
        this.processTemplateDomainSupport = new ProcessTemplateDomainSupport(
                categoryMapper,
                templateMapper,
                codeSequenceMapper,
                scopeMapper,
                customArchiveDesignMapper,
                customArchiveItemMapper,
                customArchiveRuleMapper,
                processExpenseTypeMapper,
                systemDepartmentMapper,
                userMapper,
                processFormDesignService,
                processExpenseDetailDesignService,
                processFlowDesignService,
                objectMapper
        );
        this.processCustomArchiveDomainSupport = new ProcessCustomArchiveDomainSupport(
                categoryMapper,
                templateMapper,
                codeSequenceMapper,
                scopeMapper,
                customArchiveDesignMapper,
                customArchiveItemMapper,
                customArchiveRuleMapper,
                processExpenseTypeMapper,
                systemDepartmentMapper,
                userMapper,
                processFormDesignService,
                processExpenseDetailDesignService,
                processFlowDesignService,
                objectMapper
        );
        this.processExpenseTypeDomainSupport = new ProcessExpenseTypeDomainSupport(
                categoryMapper,
                templateMapper,
                codeSequenceMapper,
                scopeMapper,
                customArchiveDesignMapper,
                customArchiveItemMapper,
                customArchiveRuleMapper,
                processExpenseTypeMapper,
                systemDepartmentMapper,
                userMapper,
                processFormDesignService,
                processExpenseDetailDesignService,
                processFlowDesignService,
                objectMapper
        );
        this.processFormDesignService = processFormDesignService;
        this.processExpenseDetailDesignService = processExpenseDetailDesignService;
        this.processFlowDesignService = processFlowDesignService;
    }

    /**
     * 获取Overview。
     */
    @Override
    public ProcessCenterOverviewVO getOverview() {
        return processCenterDomainSupport.getOverview();
    }

    /**
     * 获取模板类型。
     */
    @Override
    public List<ProcessTemplateTypeVO> getTemplateTypes() {
        return processCenterDomainSupport.getTemplateTypes();
    }

    /**
     * 获取表单选项。
     */
    @Override
    public ProcessTemplateFormOptionsVO getFormOptions(String templateType) {
        return processCenterDomainSupport.getFormOptions(templateType);
    }

    /**
     * 获取模板明细。
     */
    @Override
    public ProcessTemplateDetailVO getTemplateDetail(Long id) {
        return processTemplateDomainSupport.getTemplateDetail(id);
    }

    /**
     * 保存模板。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessTemplateSaveResultVO saveTemplate(ProcessTemplateSaveDTO dto, String operatorName) {
        return processTemplateDomainSupport.saveTemplate(dto, operatorName);
    }

    /**
     * 更新模板。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessTemplateSaveResultVO updateTemplate(Long id, ProcessTemplateSaveDTO dto, String operatorName) {
        return processTemplateDomainSupport.updateTemplate(id, dto, operatorName);
    }

    /**
     * 删除模板。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTemplate(Long id) {
        return processTemplateDomainSupport.deleteTemplate(id);
    }

    /**
     * 查询自定义档案列表。
     */
    @Override
    public List<ProcessCustomArchiveSummaryVO> listCustomArchives() {
        return processCustomArchiveDomainSupport.listCustomArchives();
    }

    /**
     * 获取自定义档案明细。
     */
    @Override
    public ProcessCustomArchiveDetailVO getCustomArchiveDetail(Long id) {
        return processCustomArchiveDomainSupport.getCustomArchiveDetail(id);
    }

    /**
     * 创建自定义档案。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessCustomArchiveDetailVO createCustomArchive(ProcessCustomArchiveSaveDTO dto) {
        return processCustomArchiveDomainSupport.createCustomArchive(dto);
    }

    /**
     * 更新自定义档案。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessCustomArchiveDetailVO updateCustomArchive(Long id, ProcessCustomArchiveSaveDTO dto) {
        return processCustomArchiveDomainSupport.updateCustomArchive(id, dto);
    }

    /**
     * 更新自定义档案Status。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCustomArchiveStatus(Long id, Integer status) {
        return processCustomArchiveDomainSupport.updateCustomArchiveStatus(id, status);
    }

    /**
     * 删除自定义档案。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCustomArchive(Long id) {
        return processCustomArchiveDomainSupport.deleteCustomArchive(id);
    }

    /**
     * 获取自定义档案元数据。
     */
    @Override
    public ProcessCustomArchiveMetaVO getCustomArchiveMeta() {
        return processCustomArchiveDomainSupport.getCustomArchiveMeta();
    }

    /**
     * 解析自定义档案。
     */
    @Override
    public ProcessCustomArchiveResolveResultVO resolveCustomArchive(ProcessCustomArchiveResolveDTO dto) {
        return processCustomArchiveDomainSupport.resolveCustomArchive(dto);
    }

    /**
     * 查询报销单类型Tree列表。
     */
    @Override
    public List<ProcessExpenseTypeTreeVO> listExpenseTypeTree() {
        return processExpenseTypeDomainSupport.listExpenseTypeTree();
    }

    /**
     * 获取报销单类型元数据。
     */
    @Override
    public ProcessExpenseTypeMetaVO getExpenseTypeMeta() {
        return processExpenseTypeDomainSupport.getExpenseTypeMeta();
    }

    /**
     * 获取报销单类型明细。
     */
    @Override
    public ProcessExpenseTypeDetailVO getExpenseTypeDetail(Long id) {
        return processExpenseTypeDomainSupport.getExpenseTypeDetail(id);
    }

    /**
     * 创建报销单类型。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessExpenseTypeDetailVO createExpenseType(ProcessExpenseTypeSaveDTO dto) {
        return processExpenseTypeDomainSupport.createExpenseType(dto);
    }

    /**
     * 更新报销单类型。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessExpenseTypeDetailVO updateExpenseType(Long id, ProcessExpenseTypeSaveDTO dto) {
        return processExpenseTypeDomainSupport.updateExpenseType(id, dto);
    }

    /**
     * 更新报销单类型Status。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateExpenseTypeStatus(Long id, Integer status) {
        return processExpenseTypeDomainSupport.updateExpenseTypeStatus(id, status);
    }

    /**
     * 删除报销单类型。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteExpenseType(Long id) {
        return processExpenseTypeDomainSupport.deleteExpenseType(id);
    }

    /**
     * 查询报销单明细设计列表。
     */
    @Override
    public List<ProcessExpenseDetailDesignSummaryVO> listExpenseDetailDesigns() {
        return processExpenseDetailDesignService.listExpenseDetailDesigns();
    }

    /**
     * 获取报销单明细设计明细。
     */
    @Override
    public ProcessExpenseDetailDesignDetailVO getExpenseDetailDesignDetail(Long id) {
        return processExpenseDetailDesignService.getExpenseDetailDesignDetail(id);
    }

    /**
     * 创建报销单明细设计。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessExpenseDetailDesignDetailVO createExpenseDetailDesign(ProcessExpenseDetailDesignSaveDTO dto) {
        return processExpenseDetailDesignService.createExpenseDetailDesign(dto);
    }

    /**
     * 更新报销单明细设计。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessExpenseDetailDesignDetailVO updateExpenseDetailDesign(Long id, ProcessExpenseDetailDesignSaveDTO dto) {
        return processExpenseDetailDesignService.updateExpenseDetailDesign(id, dto);
    }

    /**
     * 删除报销单明细设计。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteExpenseDetailDesign(Long id) {
        return processExpenseDetailDesignService.deleteExpenseDetailDesign(id);
    }

    /**
     * 查询表单设计列表。
     */
    @Override
    public List<ProcessFormDesignSummaryVO> listFormDesigns(String templateType) {
        return processFormDesignService.listFormDesigns(templateType);
    }

    /**
     * 获取表单设计明细。
     */
    @Override
    public ProcessFormDesignDetailVO getFormDesignDetail(Long id) {
        return processFormDesignService.getFormDesignDetail(id);
    }

    /**
     * 创建表单设计。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFormDesignDetailVO createFormDesign(ProcessFormDesignSaveDTO dto) {
        return processFormDesignService.createFormDesign(dto);
    }

    /**
     * 更新表单设计。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFormDesignDetailVO updateFormDesign(Long id, ProcessFormDesignSaveDTO dto) {
        return processFormDesignService.updateFormDesign(id, dto);
    }

    /**
     * 删除表单设计。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFormDesign(Long id) {
        return processFormDesignService.deleteFormDesign(id);
    }

    /**
     * 查询流程列表。
     */
    @Override
    public List<ProcessFlowSummaryVO> listFlows() {
        return processFlowDesignService.listFlows();
    }

    /**
     * 获取流程元数据。
     */
    @Override
    public ProcessFlowMetaVO getFlowMeta() {
        return processFlowDesignService.getFlowMeta();
    }

    /**
     * 获取流程明细。
     */
    @Override
    public ProcessFlowDetailVO getFlowDetail(Long id) {
        return processFlowDesignService.getFlowDetail(id);
    }

    /**
     * 创建流程。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFlowDetailVO createFlow(ProcessFlowSaveDTO dto) {
        return processFlowDesignService.createFlow(dto);
    }

    /**
     * 更新流程。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFlowDetailVO updateFlow(Long id, ProcessFlowSaveDTO dto) {
        return processFlowDesignService.updateFlow(id, dto);
    }

    /**
     * 发布流程。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFlowDetailVO publishFlow(Long id) {
        return processFlowDesignService.publishFlow(id);
    }

    /**
     * 更新流程Status。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateFlowStatus(Long id, String status) {
        return processFlowDesignService.updateFlowStatus(id, status);
    }

    /**
     * 创建流程Scene。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFlowSceneVO createFlowScene(ProcessFlowSceneSaveDTO dto) {
        return processFlowDesignService.createFlowScene(dto);
    }

    /**
     * 解析流程Approvers。
     */
    @Override
    public ProcessFlowResolveApproversVO resolveFlowApprovers(ProcessFlowResolveApproversDTO dto) {
        return processFlowDesignService.resolveApprovers(dto);
    }
    /**
     * 组装Highlights。
     */
    private List<String> buildHighlights(ProcessTemplateSaveDTO dto, java.util.Map<String, String> archiveLabelMap) {
        return processTemplateDomainSupport.buildHighlightsForTest(dto, archiveLabelMap);
    }

    /**
     * 组装模板编码。
     */
    private String buildTemplateCode() {
        return processTemplateDomainSupport.buildTemplateCodeForTest();
    }

}
