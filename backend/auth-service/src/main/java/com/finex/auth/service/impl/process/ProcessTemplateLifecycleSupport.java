package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessTemplateDetailVO;
import com.finex.auth.dto.ProcessTemplateSaveDTO;
import com.finex.auth.dto.ProcessTemplateSaveResultVO;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessTemplateScope;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public final class ProcessTemplateLifecycleSupport extends AbstractProcessTemplateSupport {

    public ProcessTemplateLifecycleSupport(
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
        super(categoryMapper, templateMapper, codeSequenceMapper, scopeMapper, customArchiveDesignMapper, customArchiveItemMapper, customArchiveRuleMapper, processExpenseTypeMapper, systemDepartmentMapper, userMapper, processFormDesignService, processExpenseDetailDesignService, processFlowDesignService, objectMapper);
    }

    public ProcessTemplateDetailVO getTemplateDetail(Long id) {
        return buildTemplateDetail(requireVisibleTemplate(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessTemplateSaveResultVO saveTemplate(ProcessTemplateSaveDTO dto, String operatorName) {
        String traceId = currentTemplateSaveTraceId();
        long startedAt = System.nanoTime();
        log.info(
                "[TemplateSaveTrace][{}][service] saveTemplate start templateName={} templateType={} category={} formDesign={} approvalFlow={} expenseDetailDesign={} enabled={}",
                traceId,
                dto.getTemplateName(),
                dto.getTemplateType(),
                dto.getCategory(),
                dto.getFormDesign(),
                dto.getApprovalFlow(),
                dto.getExpenseDetailDesign(),
                dto.getEnabled()
        );

        try {
            String categoryCode = normalize(dto.getCategory(), "employee-expense");
            String templateType = normalize(dto.getTemplateType(), "report");
            boolean enabled = dto.getEnabled() == null || dto.getEnabled();

            long stageStartedAt = System.nanoTime();
            validateTemplateScope(dto);
            logTemplateSaveStage(traceId, "saveTemplate", "validateTemplateScope", stageStartedAt);

            stageStartedAt = System.nanoTime();
            Map<String, String> departmentLabelMap = departmentLabelMap();
            Map<String, String> expenseTypeLabelMap = expenseTypeLabelMap();
            Map<String, String> archiveLabelMap = enabledArchiveLabelMap();
            Map<String, String> flowLabelMap = publishedFlowLabelMap();
            String approvalFlowCode = resolveApprovalFlowCode(dto.getApprovalFlow(), flowLabelMap);
            logTemplateSaveStage(traceId, "saveTemplate", "loadReferenceData", stageStartedAt);

            stageStartedAt = System.nanoTime();
            ProcessDocumentTemplate template = new ProcessDocumentTemplate();
            String templateCode = traceTemplateSaveValueStep(
                    traceId,
                    "saveTemplate",
                    "prepareTemplateEntity.buildTemplateCode",
                    this::buildTemplateCode
            );
            String templateTypeLabel = traceTemplateSaveValueStep(
                    traceId,
                    "saveTemplate",
                    "prepareTemplateEntity.resolveTemplateTypeLabel",
                    () -> resolveTemplateTypeLabel(templateType)
            );
            String templateDescription = traceTemplateSaveValueStep(
                    traceId,
                    "saveTemplate",
                    "prepareTemplateEntity.resolveDescription",
                    () -> resolveDescription(dto)
            );
            String formDesignCode = traceTemplateSaveValueStep(
                    traceId,
                    "saveTemplate",
                    "prepareTemplateEntity.resolveFormDesignCode",
                    () -> resolveFormDesignCode(dto.getFormDesign(), templateType)
            );
            String expenseDetailDesignCode = traceTemplateSaveValueStep(
                    traceId,
                    "saveTemplate",
                    "prepareTemplateEntity.resolveExpenseDetailDesignCode",
                    () -> resolveExpenseDetailDesignCode(dto.getExpenseDetailDesign(), templateType)
            );
            String expenseDetailModeDefault = traceTemplateSaveValueStep(
                    traceId,
                    "saveTemplate",
                    "prepareTemplateEntity.resolveExpenseDetailModeDefault",
                    () -> resolveExpenseDetailModeDefault(dto.getExpenseDetailModeDefault(), expenseDetailDesignCode)
            );
            String highlights = traceTemplateSaveValueStep(
                    traceId,
                    "saveTemplate",
                    "prepareTemplateEntity.buildHighlights",
                    () -> String.join(HIGHLIGHT_SEPARATOR, buildHighlights(dto, archiveLabelMap))
            );
            Integer sortOrder = traceTemplateSaveValueStep(
                    traceId,
                    "saveTemplate",
                    "prepareTemplateEntity.nextSortOrder",
                    () -> nextSortOrder(categoryCode)
            );
            template.setTemplateCode(templateCode);
            template.setTemplateName(trimToEmpty(dto.getTemplateName()));
            template.setTemplateType(templateType);
            template.setTemplateTypeLabel(templateTypeLabel);
            template.setCategoryCode(categoryCode);
            template.setTemplateDescription(templateDescription);
            template.setNumberingRule(DEFAULT_NUMBERING_RULE_CODE);
            template.setFormDesignCode(formDesignCode);
            template.setExpenseDetailDesignCode(expenseDetailDesignCode);
            template.setExpenseDetailModeDefault(expenseDetailModeDefault);
            template.setIconColor(DEFAULT_TEMPLATE_COLOR);
            template.setEnabled(enabled ? 1 : 0);
            template.setPublishStatus(enabled ? TEMPLATE_STATUS_ENABLED : TEMPLATE_STATUS_DRAFT);
            template.setPrintMode(normalize(dto.getPrintMode(), "default-print"));
            template.setApprovalFlow(approvalFlowCode);
            template.setFlowName(flowLabelMap.get(approvalFlowCode));
            template.setPaymentMode(normalize(dto.getPaymentMode(), "none"));
            template.setAllocationForm(normalize(dto.getAllocationForm(), "allocation-default"));
            template.setAiAuditMode(normalize(dto.getAiAuditMode(), "disabled"));
            template.setHighlights(highlights);
            template.setOwnerName(normalize(operatorName, "流程管理员"));
            template.setSortOrder(sortOrder);
            logTemplateSaveStage(traceId, "saveTemplate", "prepareTemplateEntity", stageStartedAt);

            stageStartedAt = System.nanoTime();
            insertTemplateWithRetry(template, traceId);
            log.info(
                    "[TemplateSaveTrace][{}][service] saveTemplate insertTemplate templateId={} templateCode={} costMs={}",
                    traceId,
                    template.getId(),
                    template.getTemplateCode(),
                    elapsedMillis(stageStartedAt)
            );

            stageStartedAt = System.nanoTime();
            replaceTemplateScopes(template.getId(), dto, departmentLabelMap, expenseTypeLabelMap, archiveLabelMap);
            log.info(
                    "[TemplateSaveTrace][{}][service] saveTemplate replaceTemplateScopes templateId={} costMs={}",
                    traceId,
                    template.getId(),
                    elapsedMillis(stageStartedAt)
            );

            ProcessTemplateSaveResultVO result = buildTemplateSaveResult(template);
            log.info(
                    "[TemplateSaveTrace][{}][service] saveTemplate success templateId={} templateCode={} totalMs={}",
                    traceId,
                    template.getId(),
                    template.getTemplateCode(),
                    elapsedMillis(startedAt)
            );
            return result;
        } catch (RuntimeException ex) {
            log.error(
                    "[TemplateSaveTrace][{}][service] saveTemplate failed after {}ms: {}",
                    traceId,
                    elapsedMillis(startedAt),
                    ex.getMessage(),
                    ex
            );
            throw ex;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessTemplateSaveResultVO updateTemplate(Long id, ProcessTemplateSaveDTO dto, String operatorName) {
        String traceId = currentTemplateSaveTraceId();
        long startedAt = System.nanoTime();
        log.info(
                "[TemplateSaveTrace][{}][service] updateTemplate start templateId={} templateName={} templateType={} category={} formDesign={} approvalFlow={} expenseDetailDesign={} enabled={}",
                traceId,
                id,
                dto.getTemplateName(),
                dto.getTemplateType(),
                dto.getCategory(),
                dto.getFormDesign(),
                dto.getApprovalFlow(),
                dto.getExpenseDetailDesign(),
                dto.getEnabled()
        );

        try {
            long stageStartedAt = System.nanoTime();
            ProcessDocumentTemplate template = requireVisibleTemplate(id);
            logTemplateSaveStage(traceId, "updateTemplate", "requireVisibleTemplate", stageStartedAt);

            String categoryCode = normalize(dto.getCategory(), template.getCategoryCode());
            String templateType = normalize(dto.getTemplateType(), template.getTemplateType());
            boolean enabled = dto.getEnabled() == null || dto.getEnabled();

            stageStartedAt = System.nanoTime();
            validateTemplateScope(dto);
            logTemplateSaveStage(traceId, "updateTemplate", "validateTemplateScope", stageStartedAt);

            stageStartedAt = System.nanoTime();
            Map<String, String> departmentLabelMap = departmentLabelMap();
            Map<String, String> expenseTypeLabelMap = expenseTypeLabelMap();
            Map<String, String> archiveLabelMap = enabledArchiveLabelMap();
            Map<String, String> flowLabelMap = publishedFlowLabelMap();
            String approvalFlowCode = resolveApprovalFlowCode(dto.getApprovalFlow(), flowLabelMap);
            logTemplateSaveStage(traceId, "updateTemplate", "loadReferenceData", stageStartedAt);

            stageStartedAt = System.nanoTime();
            String templateTypeLabel = traceTemplateSaveValueStep(
                    traceId,
                    "updateTemplate",
                    "prepareTemplateEntity.resolveTemplateTypeLabel",
                    () -> resolveTemplateTypeLabel(templateType)
            );
            String templateDescription = traceTemplateSaveValueStep(
                    traceId,
                    "updateTemplate",
                    "prepareTemplateEntity.resolveDescription",
                    () -> resolveDescription(dto)
            );
            String formDesignCode = traceTemplateSaveValueStep(
                    traceId,
                    "updateTemplate",
                    "prepareTemplateEntity.resolveFormDesignCode",
                    () -> resolveFormDesignCode(dto.getFormDesign(), templateType)
            );
            String expenseDetailDesignCode = traceTemplateSaveValueStep(
                    traceId,
                    "updateTemplate",
                    "prepareTemplateEntity.resolveExpenseDetailDesignCode",
                    () -> resolveExpenseDetailDesignCode(dto.getExpenseDetailDesign(), templateType)
            );
            String expenseDetailModeDefault = traceTemplateSaveValueStep(
                    traceId,
                    "updateTemplate",
                    "prepareTemplateEntity.resolveExpenseDetailModeDefault",
                    () -> resolveExpenseDetailModeDefault(dto.getExpenseDetailModeDefault(), expenseDetailDesignCode)
            );
            String highlights = traceTemplateSaveValueStep(
                    traceId,
                    "updateTemplate",
                    "prepareTemplateEntity.buildHighlights",
                    () -> String.join(HIGHLIGHT_SEPARATOR, buildHighlights(dto, archiveLabelMap))
            );
            template.setTemplateName(trimToEmpty(dto.getTemplateName()));
            template.setTemplateType(templateType);
            template.setTemplateTypeLabel(templateTypeLabel);
            template.setCategoryCode(categoryCode);
            template.setTemplateDescription(templateDescription);
            template.setNumberingRule(DEFAULT_NUMBERING_RULE_CODE);
            template.setFormDesignCode(formDesignCode);
            template.setExpenseDetailDesignCode(expenseDetailDesignCode);
            template.setExpenseDetailModeDefault(expenseDetailModeDefault);
            template.setIconColor(DEFAULT_TEMPLATE_COLOR);
            template.setEnabled(enabled ? 1 : 0);
            template.setPublishStatus(enabled ? TEMPLATE_STATUS_ENABLED : TEMPLATE_STATUS_DRAFT);
            template.setPrintMode(normalize(dto.getPrintMode(), "default-print"));
            template.setApprovalFlow(approvalFlowCode);
            template.setFlowName(flowLabelMap.get(approvalFlowCode));
            template.setPaymentMode(normalize(dto.getPaymentMode(), "none"));
            template.setAllocationForm(normalize(dto.getAllocationForm(), "allocation-default"));
            template.setAiAuditMode(normalize(dto.getAiAuditMode(), "disabled"));
            template.setHighlights(highlights);
            template.setOwnerName(normalize(operatorName, template.getOwnerName()));
            logTemplateSaveStage(traceId, "updateTemplate", "prepareTemplateEntity", stageStartedAt);

            stageStartedAt = System.nanoTime();
            getTemplateMapper().updateById(template);
            log.info(
                    "[TemplateSaveTrace][{}][service] updateTemplate updateTemplateRow templateId={} templateCode={} costMs={}",
                    traceId,
                    template.getId(),
                    template.getTemplateCode(),
                    elapsedMillis(stageStartedAt)
            );

            stageStartedAt = System.nanoTime();
            replaceTemplateScopes(template.getId(), dto, departmentLabelMap, expenseTypeLabelMap, archiveLabelMap);
            log.info(
                    "[TemplateSaveTrace][{}][service] updateTemplate replaceTemplateScopes templateId={} costMs={}",
                    traceId,
                    template.getId(),
                    elapsedMillis(stageStartedAt)
            );

            ProcessTemplateSaveResultVO result = buildTemplateSaveResult(template);
            log.info(
                    "[TemplateSaveTrace][{}][service] updateTemplate success templateId={} templateCode={} totalMs={}",
                    traceId,
                    template.getId(),
                    template.getTemplateCode(),
                    elapsedMillis(startedAt)
            );
            return result;
        } catch (RuntimeException ex) {
            log.error(
                    "[TemplateSaveTrace][{}][service] updateTemplate failed templateId={} after {}ms: {}",
                    traceId,
                    id,
                    elapsedMillis(startedAt),
                    ex.getMessage(),
                    ex
            );
            throw ex;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessTemplateSaveResultVO copyTemplate(Long id, String operatorName) {
        ProcessDocumentTemplate source = requireVisibleTemplate(id);
        Map<String, List<ProcessTemplateScope>> scopeMap = loadTemplateScopeMap(source.getId());

        ProcessTemplateSaveDTO dto = new ProcessTemplateSaveDTO();
        dto.setTemplateType(source.getTemplateType());
        dto.setTemplateName(buildTemplateCopyName(source.getTemplateName()));
        dto.setTemplateDescription(source.getTemplateDescription());
        dto.setCategory(source.getCategoryCode());
        dto.setEnabled(Boolean.FALSE);
        dto.setFormDesign(source.getFormDesignCode());
        dto.setExpenseDetailDesign(source.getExpenseDetailDesignCode());
        dto.setExpenseDetailModeDefault(source.getExpenseDetailModeDefault());
        dto.setPrintMode(source.getPrintMode());
        dto.setApprovalFlow(source.getApprovalFlow());
        dto.setPaymentMode(source.getPaymentMode());
        dto.setAllocationForm(source.getAllocationForm());
        dto.setAiAuditMode(source.getAiAuditMode());
        dto.setScopeDeptIds(new ArrayList<>(extractScopeCodes(scopeMap.get(SCOPE_TYPE_DEPARTMENT))));
        dto.setScopeExpenseTypeCodes(new ArrayList<>(extractScopeCodes(scopeMap.get(SCOPE_TYPE_EXPENSE_TYPE))));
        dto.setAmountMin(parseScopeAmount(scopeMap.get(SCOPE_TYPE_AMOUNT_MIN)));
        dto.setAmountMax(parseScopeAmount(scopeMap.get(SCOPE_TYPE_AMOUNT_MAX)));
        dto.setTagOption(resolveArchiveScopeCode(scopeMap.get(SCOPE_TYPE_TAG_ARCHIVE), scopeMap.get("TAG_OPTION")));
        dto.setInstallmentOption(resolveArchiveScopeCode(scopeMap.get(SCOPE_TYPE_INSTALLMENT_ARCHIVE), scopeMap.get("INSTALLMENT_OPTION")));
        return saveTemplate(dto, operatorName);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTemplate(Long id) {
        ProcessDocumentTemplate template = requireTemplate(id);
        template.setEnabled(0);
        template.setPublishStatus(TEMPLATE_STATUS_DELETED);
        getTemplateMapper().updateById(template);
        getScopeMapper().delete(
                Wrappers.<ProcessTemplateScope>lambdaQuery()
                        .eq(ProcessTemplateScope::getTemplateId, id)
        );
        return Boolean.TRUE;
    }

    private Map<String, String> publishedFlowLabelMap() {
        return getProcessFlowDesignService().publishedFlowLabelMap();
    }
}
