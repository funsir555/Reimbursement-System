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

@Service
public class ProcessManagementServiceImpl implements ProcessManagementService {

    private final ProcessCenterDomainSupport processCenterDomainSupport;
    private final ProcessTemplateDomainSupport processTemplateDomainSupport;
    private final ProcessCustomArchiveDomainSupport processCustomArchiveDomainSupport;
    private final ProcessExpenseTypeDomainSupport processExpenseTypeDomainSupport;
    private final ProcessFormDesignService processFormDesignService;
    private final ProcessExpenseDetailDesignService processExpenseDetailDesignService;
    private final ProcessFlowDesignService processFlowDesignService;

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

    @Override
    public ProcessCenterOverviewVO getOverview() {
        return processCenterDomainSupport.getOverview();
    }

    @Override
    public List<ProcessTemplateTypeVO> getTemplateTypes() {
        return processCenterDomainSupport.getTemplateTypes();
    }

    @Override
    public ProcessTemplateFormOptionsVO getFormOptions(String templateType) {
        return processCenterDomainSupport.getFormOptions(templateType);
    }

    @Override
    public ProcessTemplateDetailVO getTemplateDetail(Long id) {
        return processTemplateDomainSupport.getTemplateDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessTemplateSaveResultVO saveTemplate(ProcessTemplateSaveDTO dto, String operatorName) {
        return processTemplateDomainSupport.saveTemplate(dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessTemplateSaveResultVO updateTemplate(Long id, ProcessTemplateSaveDTO dto, String operatorName) {
        return processTemplateDomainSupport.updateTemplate(id, dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTemplate(Long id) {
        return processTemplateDomainSupport.deleteTemplate(id);
    }

    @Override
    public List<ProcessCustomArchiveSummaryVO> listCustomArchives() {
        return processCustomArchiveDomainSupport.listCustomArchives();
    }

    @Override
    public ProcessCustomArchiveDetailVO getCustomArchiveDetail(Long id) {
        return processCustomArchiveDomainSupport.getCustomArchiveDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessCustomArchiveDetailVO createCustomArchive(ProcessCustomArchiveSaveDTO dto) {
        return processCustomArchiveDomainSupport.createCustomArchive(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessCustomArchiveDetailVO updateCustomArchive(Long id, ProcessCustomArchiveSaveDTO dto) {
        return processCustomArchiveDomainSupport.updateCustomArchive(id, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCustomArchiveStatus(Long id, Integer status) {
        return processCustomArchiveDomainSupport.updateCustomArchiveStatus(id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCustomArchive(Long id) {
        return processCustomArchiveDomainSupport.deleteCustomArchive(id);
    }

    @Override
    public ProcessCustomArchiveMetaVO getCustomArchiveMeta() {
        return processCustomArchiveDomainSupport.getCustomArchiveMeta();
    }

    @Override
    public ProcessCustomArchiveResolveResultVO resolveCustomArchive(ProcessCustomArchiveResolveDTO dto) {
        return processCustomArchiveDomainSupport.resolveCustomArchive(dto);
    }

    @Override
    public List<ProcessExpenseTypeTreeVO> listExpenseTypeTree() {
        return processExpenseTypeDomainSupport.listExpenseTypeTree();
    }

    @Override
    public ProcessExpenseTypeMetaVO getExpenseTypeMeta() {
        return processExpenseTypeDomainSupport.getExpenseTypeMeta();
    }

    @Override
    public ProcessExpenseTypeDetailVO getExpenseTypeDetail(Long id) {
        return processExpenseTypeDomainSupport.getExpenseTypeDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessExpenseTypeDetailVO createExpenseType(ProcessExpenseTypeSaveDTO dto) {
        return processExpenseTypeDomainSupport.createExpenseType(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessExpenseTypeDetailVO updateExpenseType(Long id, ProcessExpenseTypeSaveDTO dto) {
        return processExpenseTypeDomainSupport.updateExpenseType(id, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateExpenseTypeStatus(Long id, Integer status) {
        return processExpenseTypeDomainSupport.updateExpenseTypeStatus(id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteExpenseType(Long id) {
        return processExpenseTypeDomainSupport.deleteExpenseType(id);
    }

    @Override
    public List<ProcessExpenseDetailDesignSummaryVO> listExpenseDetailDesigns() {
        return processExpenseDetailDesignService.listExpenseDetailDesigns();
    }

    @Override
    public ProcessExpenseDetailDesignDetailVO getExpenseDetailDesignDetail(Long id) {
        return processExpenseDetailDesignService.getExpenseDetailDesignDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessExpenseDetailDesignDetailVO createExpenseDetailDesign(ProcessExpenseDetailDesignSaveDTO dto) {
        return processExpenseDetailDesignService.createExpenseDetailDesign(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessExpenseDetailDesignDetailVO updateExpenseDetailDesign(Long id, ProcessExpenseDetailDesignSaveDTO dto) {
        return processExpenseDetailDesignService.updateExpenseDetailDesign(id, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteExpenseDetailDesign(Long id) {
        return processExpenseDetailDesignService.deleteExpenseDetailDesign(id);
    }

    @Override
    public List<ProcessFormDesignSummaryVO> listFormDesigns(String templateType) {
        return processFormDesignService.listFormDesigns(templateType);
    }

    @Override
    public ProcessFormDesignDetailVO getFormDesignDetail(Long id) {
        return processFormDesignService.getFormDesignDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFormDesignDetailVO createFormDesign(ProcessFormDesignSaveDTO dto) {
        return processFormDesignService.createFormDesign(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFormDesignDetailVO updateFormDesign(Long id, ProcessFormDesignSaveDTO dto) {
        return processFormDesignService.updateFormDesign(id, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFormDesign(Long id) {
        return processFormDesignService.deleteFormDesign(id);
    }

    @Override
    public List<ProcessFlowSummaryVO> listFlows() {
        return processFlowDesignService.listFlows();
    }

    @Override
    public ProcessFlowMetaVO getFlowMeta() {
        return processFlowDesignService.getFlowMeta();
    }

    @Override
    public ProcessFlowDetailVO getFlowDetail(Long id) {
        return processFlowDesignService.getFlowDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFlowDetailVO createFlow(ProcessFlowSaveDTO dto) {
        return processFlowDesignService.createFlow(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFlowDetailVO updateFlow(Long id, ProcessFlowSaveDTO dto) {
        return processFlowDesignService.updateFlow(id, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFlowDetailVO publishFlow(Long id) {
        return processFlowDesignService.publishFlow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateFlowStatus(Long id, String status) {
        return processFlowDesignService.updateFlowStatus(id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFlowSceneVO createFlowScene(ProcessFlowSceneSaveDTO dto) {
        return processFlowDesignService.createFlowScene(dto);
    }

    @Override
    public ProcessFlowResolveApproversVO resolveFlowApprovers(ProcessFlowResolveApproversDTO dto) {
        return processFlowDesignService.resolveApprovers(dto);
    }
    private List<String> buildHighlights(ProcessTemplateSaveDTO dto, java.util.Map<String, String> archiveLabelMap) {
        return processTemplateDomainSupport.buildHighlightsForTest(dto, archiveLabelMap);
    }

    private String buildTemplateCode() {
        return processTemplateDomainSupport.buildTemplateCodeForTest();
    }

}
