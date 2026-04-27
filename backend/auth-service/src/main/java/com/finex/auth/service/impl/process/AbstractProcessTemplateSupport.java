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
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
abstract class AbstractProcessTemplateSupport extends AbstractProcessManagementSupport {

    private static final DateTimeFormatter TEMPLATE_CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String TEMPLATE_CODE_PREFIX = "FX";
    private static final String TEMPLATE_CODE_SEQUENCE_KEY = "DOCUMENT_TEMPLATE";
    private static final int TEMPLATE_CODE_RETRY_LIMIT = 3;
    private static final int PM_NAME_MAX_LENGTH = 64;

    private ProcessTemplateBindingSupport processTemplateBindingSupport;

    protected AbstractProcessTemplateSupport(
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
        super(
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
    }

    protected ProcessTemplateDetailVO buildTemplateDetail(ProcessDocumentTemplate template) {
        return processTemplateBindingSupport().buildTemplateDetail(template);
    }

    protected ProcessTemplateSaveResultVO buildTemplateSaveResult(ProcessDocumentTemplate template) {
        ProcessTemplateSaveResultVO result = new ProcessTemplateSaveResultVO();
        result.setId(template.getId());
        result.setTemplateCode(template.getTemplateCode());
        result.setTemplateName(template.getTemplateName());
        result.setStatus(template.getPublishStatus());
        return result;
    }

    protected Map<String, List<ProcessTemplateScope>> loadTemplateScopeMap(Long templateId) {
        return processTemplateBindingSupport().loadTemplateScopeMap(templateId);
    }

    protected List<String> extractScopeCodes(List<ProcessTemplateScope> scopes) {
        return processTemplateBindingSupport().extractScopeCodes(scopes);
    }

    protected BigDecimal parseScopeAmount(List<ProcessTemplateScope> scopes) {
        return processTemplateBindingSupport().parseScopeAmount(scopes);
    }

    protected String firstScopeCode(List<ProcessTemplateScope> scopes) {
        return processTemplateBindingSupport().firstScopeCode(scopes);
    }

    protected String resolveArchiveScopeCode(List<ProcessTemplateScope> archiveScopes, List<ProcessTemplateScope> legacyScopes) {
        return processTemplateBindingSupport().resolveArchiveScopeCode(archiveScopes, legacyScopes);
    }

    protected String findArchiveCodeByLegacyItemCode(String itemCode) {
        return processTemplateBindingSupport().findArchiveCodeByLegacyItemCode(itemCode);
    }

    protected String resolveFormDesignCode(String formDesign, String templateType) {
        return processTemplateBindingSupport().resolveFormDesignCode(formDesign, templateType);
    }

    protected String resolveExpenseDetailDesignCode(String expenseDetailDesign, String templateType) {
        return processTemplateBindingSupport().resolveExpenseDetailDesignCode(expenseDetailDesign, templateType);
    }

    protected String resolveExpenseDetailType(String expenseDetailDesignCode) {
        return processTemplateBindingSupport().resolveExpenseDetailType(expenseDetailDesignCode);
    }

    protected String resolveExpenseDetailModeDefault(String expenseDetailModeDefault, String expenseDetailDesignCode) {
        return processTemplateBindingSupport().resolveExpenseDetailModeDefault(expenseDetailModeDefault, expenseDetailDesignCode);
    }

    protected void validateTemplateScope(ProcessTemplateSaveDTO dto) {
        processTemplateBindingSupport().validateTemplateScope(dto);
    }

    protected List<String> buildHighlights(ProcessTemplateSaveDTO dto, Map<String, String> archiveLabelMap) {
        return processTemplateBindingSupport().buildHighlights(dto, archiveLabelMap);
    }

    protected void saveScopeItems(Long templateId, String optionType, List<String> codes, Map<String, String> labelMap) {
        processTemplateBindingSupport().saveScopeItems(templateId, optionType, codes, labelMap);
    }

    protected void saveSingleScopeItem(Long templateId, String optionType, String code, Map<String, String> labelMap) {
        processTemplateBindingSupport().saveSingleScopeItem(templateId, optionType, code, labelMap);
    }

    protected void replaceTemplateScopes(
            Long templateId,
            ProcessTemplateSaveDTO dto,
            Map<String, String> departmentLabelMap,
            Map<String, String> expenseTypeLabelMap,
            Map<String, String> archiveLabelMap
    ) {
        processTemplateBindingSupport().replaceTemplateScopes(templateId, dto, departmentLabelMap, expenseTypeLabelMap, archiveLabelMap);
    }

    protected int nextSortOrder(String categoryCode) {
        List<ProcessDocumentTemplate> templates = getTemplateMapper().selectList(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .eq(ProcessDocumentTemplate::getCategoryCode, categoryCode)
                        .orderByDesc(ProcessDocumentTemplate::getSortOrder, ProcessDocumentTemplate::getId)
                        .last("limit 1")
        );
        if (templates.isEmpty() || templates.get(0).getSortOrder() == null) {
            return 1;
        }
        return templates.get(0).getSortOrder() + 1;
    }

    protected String resolveDescription(ProcessTemplateSaveDTO dto) {
        String description = trimToNull(dto.getTemplateDescription());
        if (description != null) {
            return description;
        }
        return resolveTemplateTypeLabel(dto.getTemplateType()) + "\u6a21\u677f";
    }

    protected String resolveTemplateTypeLabel(String templateType) {
        return switch (normalize(templateType, "report")) {
            case "application" -> "\u7533\u8bf7\u5355";
            case "loan" -> "\u501f\u6b3e\u5355";
            case "contract" -> "\u5408\u540c\u5355";
            default -> "\u62a5\u9500\u5355";
        };
    }

    protected String resolveApprovalFlowCode(String approvalFlow, Map<String, String> flowLabelMap) {
        String flowCode = trimToNull(approvalFlow);
        if (flowCode == null) {
            throw new IllegalArgumentException("\u8bf7\u9009\u62e9\u5ba1\u6279\u6d41\u7a0b");
        }
        if (!flowLabelMap.containsKey(flowCode)) {
            throw new IllegalArgumentException("\u5ba1\u6279\u6d41\u7a0b\u4e0d\u5b58\u5728\u6216\u5c1a\u672a\u53d1\u5e03");
        }
        return flowCode;
    }

    protected String buildTemplateCode() {
        String bizDate = LocalDate.now().format(TEMPLATE_CODE_DATE_FORMATTER);
        String prefix = TEMPLATE_CODE_PREFIX + bizDate;
        for (int attempt = 1; attempt <= TEMPLATE_CODE_RETRY_LIMIT; attempt++) {
            long nextValue = nextTemplateCodeSequenceValue(bizDate, prefix);
            String templateCode = prefix + String.format("%04d", nextValue);
            if (!templateCodeExists(templateCode)) {
                return templateCode;
            }
            log.warn("[TemplateSaveTrace][{}][service] buildTemplateCode collision attempt={} templateCode={}", currentTemplateSaveTraceId(), attempt, templateCode);
        }
        throw new IllegalArgumentException("\u6a21\u677f\u7f16\u7801\u751f\u6210\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5");
    }

    protected void insertTemplateWithRetry(ProcessDocumentTemplate template, String traceId) {
        for (int attempt = 1; attempt <= TEMPLATE_CODE_RETRY_LIMIT; attempt++) {
            try {
                getTemplateMapper().insert(template);
                return;
            } catch (DuplicateKeyException ex) {
                if (!isTemplateCodeDuplicate(ex)) {
                    throw ex;
                }
                if (attempt >= TEMPLATE_CODE_RETRY_LIMIT) {
                    throw new IllegalArgumentException("\u6a21\u677f\u7f16\u7801\u751f\u6210\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5", ex);
                }
                log.warn("[TemplateSaveTrace][{}][service] saveTemplate insertTemplate duplicate templateCode={} attempt={} - regenerating", traceId, template.getTemplateCode(), attempt);
                template.setTemplateCode(buildTemplateCode());
            }
        }
    }

    protected Map<String, String> expenseTypeLabelMap() {
        return processTemplateBindingSupport().expenseTypeLabelMap();
    }

    protected Map<String, String> departmentLabelMap() {
        return processTemplateBindingSupport().departmentLabelMap();
    }

    protected void saveAmountScopeItems(Long templateId, BigDecimal amountMin, BigDecimal amountMax) {
        processTemplateBindingSupport().saveAmountScopeItems(templateId, amountMin, amountMax);
    }

    protected Map<String, String> enabledArchiveLabelMap() {
        return processTemplateBindingSupport().enabledArchiveLabelMap();
    }

    protected ProcessDocumentTemplate requireTemplate(Long id) {
        ProcessDocumentTemplate template = getTemplateMapper().selectById(id);
        if (template == null) {
            throw new IllegalStateException("\u6a21\u677f\u4e0d\u5b58\u5728");
        }
        return template;
    }

    protected ProcessDocumentTemplate requireVisibleTemplate(Long id) {
        ProcessDocumentTemplate template = requireTemplate(id);
        if (TEMPLATE_STATUS_DELETED.equals(template.getPublishStatus())) {
            throw new IllegalStateException("\u6a21\u677f\u5df2\u5220\u9664\uff0c\u65e0\u6cd5\u7ee7\u7eed\u64cd\u4f5c");
        }
        return template;
    }

    protected String buildTemplateCopyName(String sourceName) {
        String baseName = trimToNull(sourceName);
        if (baseName == null) {
            baseName = "\u672a\u547d\u540d\u6a21\u677f";
        }
        String normalizedBase = trimTemplateCopyBase(baseName);
        String candidate = buildTemplateCopyCandidate(normalizedBase, TEMPLATE_COPY_SUFFIX);
        if (!templateNameExists(candidate)) {
            return candidate;
        }
        for (int index = 2; index < 1000; index++) {
            candidate = buildTemplateCopyCandidate(normalizedBase, TEMPLATE_COPY_SUFFIX + index);
            if (!templateNameExists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("\u6a21\u677f\u526f\u672c\u540d\u79f0\u751f\u6210\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5");
    }

    protected ProcessTemplateBindingSupport getProcessTemplateBindingSupport() {
        return processTemplateBindingSupport();
    }

    private long nextTemplateCodeSequenceValue(String bizDate, String prefix) {
        int updatedRows = getCodeSequenceMapper().allocateNextTemplateCodeValue(TEMPLATE_CODE_SEQUENCE_KEY, bizDate);
        if (updatedRows == 0) {
            initializeTemplateCodeSequence(bizDate, prefix);
            updatedRows = getCodeSequenceMapper().allocateNextTemplateCodeValue(TEMPLATE_CODE_SEQUENCE_KEY, bizDate);
        }
        if (updatedRows == 0) {
            throw new IllegalStateException("\u6a21\u677f\u7f16\u7801\u5e8f\u5217\u7533\u8bf7\u5931\u8d25");
        }
        Long currentValue = getCodeSequenceMapper().currentAllocatedValue();
        if (currentValue == null || currentValue < 1L) {
            throw new IllegalStateException("\u6a21\u677f\u7f16\u7801\u5e8f\u5217\u8fd4\u56de\u975e\u6cd5\u503c");
        }
        return currentValue;
    }

    private void initializeTemplateCodeSequence(String bizDate, String prefix) {
        long initialValue = currentTemplateCodeSequenceValue(prefix);
        getCodeSequenceMapper().initializeSequenceIfAbsent(TEMPLATE_CODE_SEQUENCE_KEY, bizDate, initialValue);
    }

    private long currentTemplateCodeSequenceValue(String prefix) {
        Long currentValue = getTemplateMapper().selectMaxTemplateCodeValueByPrefix(prefix);
        if (currentValue == null || currentValue < 0L) {
            return 0L;
        }
        return currentValue;
    }

    private boolean templateCodeExists(String templateCode) {
        Long count = getTemplateMapper().selectCount(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .eq(ProcessDocumentTemplate::getTemplateCode, templateCode)
        );
        return count != null && count > 0;
    }

    private boolean isTemplateCodeDuplicate(DuplicateKeyException ex) {
        String message = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("uk_template_code") || normalized.contains("pm_document_template.uk_template_code");
    }

    private String buildTemplateCopyCandidate(String baseName, String suffix) {
        String normalizedBase = trimToNull(baseName);
        String normalizedSuffix = suffix == null ? null : suffix;
        if (normalizedBase == null) {
            normalizedBase = "\u672a\u547d\u540d\u6a21\u677f";
        }
        if (normalizedSuffix == null || normalizedSuffix.isBlank()) {
            return trimTemplateNameToLength(normalizedBase);
        }
        int maxBaseLength = Math.max(1, PM_NAME_MAX_LENGTH - normalizedSuffix.length());
        String trimmedBase = normalizedBase.length() <= maxBaseLength
                ? normalizedBase
                : normalizedBase.substring(0, maxBaseLength).trim();
        if (trimmedBase.isEmpty()) {
            trimmedBase = normalizedBase.substring(0, Math.min(normalizedBase.length(), maxBaseLength));
        }
        return trimmedBase + normalizedSuffix;
    }

    private String trimTemplateCopyBase(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "\u672a\u547d\u540d\u6a21\u677f";
        }
        if (normalized.endsWith(TEMPLATE_COPY_SUFFIX)) {
            return normalized.substring(0, normalized.length() - TEMPLATE_COPY_SUFFIX.length()).trim();
        }
        for (int index = normalized.length() - 1; index >= 0; index--) {
            if (!Character.isDigit(normalized.charAt(index))) {
                String prefix = normalized.substring(0, index + 1);
                String suffix = normalized.substring(index + 1);
                if (prefix.endsWith(TEMPLATE_COPY_SUFFIX) && !suffix.isEmpty()) {
                    return prefix.substring(0, prefix.length() - TEMPLATE_COPY_SUFFIX.length()).trim();
                }
                break;
            }
        }
        return normalized;
    }

    private String trimTemplateNameToLength(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "\u672a\u547d\u540d\u6a21\u677f";
        }
        return normalized.length() <= PM_NAME_MAX_LENGTH
                ? normalized
                : normalized.substring(0, PM_NAME_MAX_LENGTH).trim();
    }

    private boolean templateNameExists(String templateName) {
        String normalized = trimToNull(templateName);
        if (normalized == null) {
            return false;
        }
        Long count = getTemplateMapper().selectCount(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .eq(ProcessDocumentTemplate::getTemplateName, normalized)
                        .ne(ProcessDocumentTemplate::getPublishStatus, TEMPLATE_STATUS_DELETED)
        );
        return count != null && count > 0;
    }

    private ProcessTemplateBindingSupport processTemplateBindingSupport() {
        if (processTemplateBindingSupport == null) {
            processTemplateBindingSupport = new ProcessTemplateBindingSupport(
                    this,
                    getScopeMapper(),
                    getCustomArchiveDesignMapper(),
                    getCustomArchiveItemMapper(),
                    getProcessExpenseTypeMapper(),
                    getSystemDepartmentMapper(),
                    getProcessFormDesignService(),
                    getProcessExpenseDetailDesignService(),
                    getProcessFlowDesignService()
            );
        }
        return processTemplateBindingSupport;
    }
}
