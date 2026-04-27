package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ProcessExpenseDetailDesignSummaryVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.dto.ProcessTemplateDetailVO;
import com.finex.auth.dto.ProcessTemplateSaveDTO;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessCustomArchiveItem;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseType;
import com.finex.auth.entity.ProcessTemplateScope;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessCustomArchiveItemMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessTemplateScopeMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.service.ProcessExpenseDetailDesignService;
import com.finex.auth.service.ProcessFlowDesignService;
import com.finex.auth.service.ProcessFormDesignService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

final class ProcessTemplateBindingSupport {

    private final AbstractProcessTemplateSupport owner;
    private final ProcessTemplateScopeMapper scopeMapper;
    private final ProcessCustomArchiveDesignMapper customArchiveDesignMapper;
    private final ProcessCustomArchiveItemMapper customArchiveItemMapper;
    private final ProcessExpenseTypeMapper processExpenseTypeMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;
    private final ProcessFormDesignService processFormDesignService;
    private final ProcessExpenseDetailDesignService processExpenseDetailDesignService;
    private final ProcessFlowDesignService processFlowDesignService;

    ProcessTemplateBindingSupport(
            AbstractProcessTemplateSupport owner,
            ProcessTemplateScopeMapper scopeMapper,
            ProcessCustomArchiveDesignMapper customArchiveDesignMapper,
            ProcessCustomArchiveItemMapper customArchiveItemMapper,
            ProcessExpenseTypeMapper processExpenseTypeMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            ProcessFormDesignService processFormDesignService,
            ProcessExpenseDetailDesignService processExpenseDetailDesignService,
            ProcessFlowDesignService processFlowDesignService
    ) {
        this.owner = owner;
        this.scopeMapper = scopeMapper;
        this.customArchiveDesignMapper = customArchiveDesignMapper;
        this.customArchiveItemMapper = customArchiveItemMapper;
        this.processExpenseTypeMapper = processExpenseTypeMapper;
        this.systemDepartmentMapper = systemDepartmentMapper;
        this.processFormDesignService = processFormDesignService;
        this.processExpenseDetailDesignService = processExpenseDetailDesignService;
        this.processFlowDesignService = processFlowDesignService;
    }

    ProcessTemplateDetailVO buildTemplateDetail(ProcessDocumentTemplate template) {
        Map<String, List<ProcessTemplateScope>> scopeMap = loadTemplateScopeMap(template.getId());

        ProcessTemplateDetailVO detail = new ProcessTemplateDetailVO();
        detail.setId(template.getId());
        detail.setTemplateCode(template.getTemplateCode());
        detail.setTemplateType(template.getTemplateType());
        detail.setTemplateTypeLabel(owner.normalize(template.getTemplateTypeLabel(), owner.resolveTemplateTypeLabel(template.getTemplateType())));
        detail.setTemplateName(template.getTemplateName());
        detail.setTemplateDescription(template.getTemplateDescription());
        detail.setCategory(template.getCategoryCode());
        detail.setEnabled(template.getEnabled() == null || template.getEnabled() == 1);
        detail.setFormDesign(template.getFormDesignCode());
        detail.setExpenseDetailDesign(template.getExpenseDetailDesignCode());
        detail.setExpenseDetailType(resolveExpenseDetailType(template.getExpenseDetailDesignCode()));
        detail.setExpenseDetailModeDefault(template.getExpenseDetailModeDefault());
        detail.setPrintMode(template.getPrintMode());
        detail.setApprovalFlow(template.getApprovalFlow());
        detail.setPaymentMode(template.getPaymentMode());
        detail.setAllocationForm(template.getAllocationForm());
        detail.setAiAuditMode(template.getAiAuditMode());
        detail.setScopeDeptIds(extractScopeCodes(scopeMap.get(AbstractProcessManagementSupport.SCOPE_TYPE_DEPARTMENT)));
        detail.setScopeExpenseTypeCodes(extractScopeCodes(scopeMap.get(AbstractProcessManagementSupport.SCOPE_TYPE_EXPENSE_TYPE)));
        detail.setAmountMin(parseScopeAmount(scopeMap.get(AbstractProcessManagementSupport.SCOPE_TYPE_AMOUNT_MIN)));
        detail.setAmountMax(parseScopeAmount(scopeMap.get(AbstractProcessManagementSupport.SCOPE_TYPE_AMOUNT_MAX)));
        detail.setTagOption(resolveArchiveScopeCode(scopeMap.get(AbstractProcessManagementSupport.SCOPE_TYPE_TAG_ARCHIVE), scopeMap.get("TAG_OPTION")));
        detail.setInstallmentOption(resolveArchiveScopeCode(scopeMap.get(AbstractProcessManagementSupport.SCOPE_TYPE_INSTALLMENT_ARCHIVE), scopeMap.get("INSTALLMENT_OPTION")));
        return detail;
    }

    Map<String, List<ProcessTemplateScope>> loadTemplateScopeMap(Long templateId) {
        return scopeMapper.selectList(
                Wrappers.<ProcessTemplateScope>lambdaQuery()
                        .eq(ProcessTemplateScope::getTemplateId, templateId)
                        .orderByAsc(ProcessTemplateScope::getSortOrder, ProcessTemplateScope::getId)
        ).stream().collect(Collectors.groupingBy(
                ProcessTemplateScope::getOptionType,
                LinkedHashMap::new,
                Collectors.toList()
        ));
    }

    List<String> extractScopeCodes(List<ProcessTemplateScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return Collections.emptyList();
        }
        return scopes.stream()
                .map(ProcessTemplateScope::getOptionCode)
                .filter(Objects::nonNull)
                .toList();
    }

    BigDecimal parseScopeAmount(List<ProcessTemplateScope> scopes) {
        String value = firstScopeCode(scopes);
        if (value == null) {
            return null;
        }
        return new BigDecimal(value);
    }

    String firstScopeCode(List<ProcessTemplateScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return null;
        }
        return owner.trimToNull(scopes.get(0).getOptionCode());
    }

    String resolveArchiveScopeCode(List<ProcessTemplateScope> archiveScopes, List<ProcessTemplateScope> legacyScopes) {
        String archiveCode = firstScopeCode(archiveScopes);
        if (archiveCode != null) {
            return archiveCode;
        }

        String legacyItemCode = firstScopeCode(legacyScopes);
        if (legacyItemCode == null) {
            return "";
        }
        return owner.normalize(findArchiveCodeByLegacyItemCode(legacyItemCode), "");
    }

    String findArchiveCodeByLegacyItemCode(String itemCode) {
        ProcessCustomArchiveItem item = customArchiveItemMapper.selectOne(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .eq(ProcessCustomArchiveItem::getItemCode, itemCode)
                        .last("limit 1")
        );
        if (item == null) {
            return null;
        }

        ProcessCustomArchiveDesign archive = customArchiveDesignMapper.selectById(item.getArchiveId());
        return archive == null ? null : archive.getArchiveCode();
    }

    String resolveFormDesignCode(String formDesign, String templateType) {
        return processFormDesignService.resolveFormDesignCode(formDesign, templateType);
    }

    String resolveExpenseDetailDesignCode(String expenseDetailDesign, String templateType) {
        String normalizedTemplateType = owner.normalize(templateType, "report");
        String normalizedCode = owner.trimToNull(expenseDetailDesign);
        if (!Objects.equals(normalizedTemplateType, "report")) {
            if (normalizedCode != null) {
                throw new IllegalArgumentException("只有报销模板支持绑定费用明细表单");
            }
            return null;
        }
        return processExpenseDetailDesignService.resolveExpenseDetailDesignCode(normalizedCode);
    }

    String resolveExpenseDetailType(String expenseDetailDesignCode) {
        String normalizedCode = owner.trimToNull(expenseDetailDesignCode);
        return normalizedCode == null ? null : processExpenseDetailDesignService.resolveExpenseDetailType(normalizedCode);
    }

    String resolveExpenseDetailModeDefault(String expenseDetailModeDefault, String expenseDetailDesignCode) {
        String detailType = resolveExpenseDetailType(expenseDetailDesignCode);
        if (!Objects.equals(detailType, "ENTERPRISE_TRANSACTION")) {
            return null;
        }
        String normalizedMode = owner.trimToNull(expenseDetailModeDefault);
        if (normalizedMode == null) {
            return null;
        }
        if (!Objects.equals(normalizedMode, "PREPAY_UNBILLED") && !Objects.equals(normalizedMode, "INVOICE_FULL_PAYMENT")) {
            throw new IllegalArgumentException("企业往来费用明细默认模式不合法");
        }
        return normalizedMode;
    }

    void validateTemplateScope(ProcessTemplateSaveDTO dto) {
        String templateType = owner.normalize(dto.getTemplateType(), "report");
        owner.validatePmNameLength(dto.getTemplateName(), "单据名称");
        owner.validateSelectableIds(owner.normalizeIdList(dto.getScopeDeptIds()), owner.loadValidDepartmentIdSet(), "部门");
        owner.validateSelectableIds(owner.normalizeIdList(dto.getScopeExpenseTypeCodes()), owner.loadValidExpenseTypeCodeSet(), "费用类型");
        resolveFormDesignCode(dto.getFormDesign(), templateType);
        owner.resolveApprovalFlowCode(dto.getApprovalFlow(), processFlowDesignService.publishedFlowLabelMap());

        if (Objects.equals(templateType, "report")) {
            if (owner.trimToNull(dto.getExpenseDetailDesign()) == null) {
                throw new IllegalArgumentException("报销模板必须绑定费用明细表单");
            }
            resolveExpenseDetailDesignCode(dto.getExpenseDetailDesign(), templateType);
        } else if (owner.trimToNull(dto.getExpenseDetailDesign()) != null || owner.trimToNull(dto.getExpenseDetailModeDefault()) != null) {
            throw new IllegalArgumentException("申请单和借款单不支持费用明细表单");
        }

        BigDecimal amountMin = dto.getAmountMin();
        BigDecimal amountMax = dto.getAmountMax();
        if (amountMin != null && amountMin.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("最小金额不能小于 0");
        }
        if (amountMax != null && amountMax.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("最大金额不能小于 0");
        }
        if (amountMin != null && amountMax != null && amountMin.compareTo(amountMax) > 0) {
            throw new IllegalArgumentException("限定金额区间不合法，最小金额不能大于最大金额");
        }
    }

    List<String> buildHighlights(ProcessTemplateSaveDTO dto, Map<String, String> archiveLabelMap) {
        LinkedHashSet<String> uniqueHighlights = new LinkedHashSet<>();
        uniqueHighlights.add("移动端提单");
        if (!"none".equalsIgnoreCase(owner.normalize(dto.getPaymentMode(), "none"))) {
            uniqueHighlights.add("付款单联动");
        }
        if (!"disabled".equalsIgnoreCase(owner.normalize(dto.getAiAuditMode(), "disabled"))) {
            uniqueHighlights.add("AI 审核");
        }

        String tagLabel = archiveLabelMap.get(owner.trimToEmpty(dto.getTagOption()));
        if (tagLabel != null) {
            uniqueHighlights.add(tagLabel);
        }
        String installmentLabel = archiveLabelMap.get(owner.trimToEmpty(dto.getInstallmentOption()));
        if (installmentLabel != null) {
            uniqueHighlights.add(installmentLabel);
        }

        List<String> highlights = new ArrayList<>(uniqueHighlights);
        while (highlights.size() < 3) {
            highlights.add("暂无亮点");
        }
        return highlights.stream().limit(3).toList();
    }

    void replaceTemplateScopes(
            Long templateId,
            ProcessTemplateSaveDTO dto,
            Map<String, String> departmentLabelMap,
            Map<String, String> expenseTypeLabelMap,
            Map<String, String> archiveLabelMap
    ) {
        scopeMapper.delete(
                Wrappers.<ProcessTemplateScope>lambdaQuery()
                        .eq(ProcessTemplateScope::getTemplateId, templateId)
        );
        saveScopeItems(templateId, AbstractProcessManagementSupport.SCOPE_TYPE_DEPARTMENT, dto.getScopeDeptIds(), departmentLabelMap);
        saveScopeItems(templateId, AbstractProcessManagementSupport.SCOPE_TYPE_EXPENSE_TYPE, dto.getScopeExpenseTypeCodes(), expenseTypeLabelMap);
        saveAmountScopeItems(templateId, dto.getAmountMin(), dto.getAmountMax());
        saveSingleScopeItem(templateId, AbstractProcessManagementSupport.SCOPE_TYPE_TAG_ARCHIVE, dto.getTagOption(), archiveLabelMap);
        saveSingleScopeItem(templateId, AbstractProcessManagementSupport.SCOPE_TYPE_INSTALLMENT_ARCHIVE, dto.getInstallmentOption(), archiveLabelMap);
    }

    void saveScopeItems(Long templateId, String optionType, List<String> codes, Map<String, String> labelMap) {
        persistScopeItems(templateId, optionType, codes, labelMap);
    }

    void saveSingleScopeItem(Long templateId, String optionType, String code, Map<String, String> labelMap) {
        persistSingleScopeItem(templateId, optionType, code, labelMap);
    }

    void saveAmountScopeItems(Long templateId, BigDecimal amountMin, BigDecimal amountMax) {
        persistAmountScopeItems(templateId, amountMin, amountMax);
    }

    Map<String, String> expenseTypeLabelMap() {
        return processExpenseTypeMapper.selectList(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .orderByAsc(ProcessExpenseType::getExpenseCode, ProcessExpenseType::getId)
        ).stream().collect(Collectors.toMap(
                ProcessExpenseType::getExpenseCode,
                ProcessExpenseType::getExpenseName,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    Map<String, String> departmentLabelMap() {
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .select(SystemDepartment::getId, SystemDepartment::getDeptName)
                        .eq(SystemDepartment::getStatus, 1)
                        .orderByAsc(SystemDepartment::getSortOrder, SystemDepartment::getId)
        ).stream().map(department -> option(department.getDeptName(), String.valueOf(department.getId())))
                .collect(Collectors.toMap(
                        ProcessFormOptionVO::getValue,
                        ProcessFormOptionVO::getLabel,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    Map<String, String> enabledArchiveLabelMap() {
        return customArchiveDesignMapper.selectList(
                Wrappers.<ProcessCustomArchiveDesign>lambdaQuery()
                        .eq(ProcessCustomArchiveDesign::getStatus, 1)
                        .orderByDesc(ProcessCustomArchiveDesign::getUpdatedAt, ProcessCustomArchiveDesign::getId)
        ).stream().map(archive -> option(archive.getArchiveName(), archive.getArchiveCode()))
                .collect(Collectors.toMap(
                        ProcessFormOptionVO::getValue,
                        ProcessFormOptionVO::getLabel,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private void persistScopeItems(Long templateId, String optionType, List<String> codes, Map<String, String> labelMap) {
        List<String> normalizedCodes = owner.normalizeIdList(codes);
        for (int index = 0; index < normalizedCodes.size(); index++) {
            String code = normalizedCodes.get(index);
            ProcessTemplateScope scope = new ProcessTemplateScope();
            scope.setTemplateId(templateId);
            scope.setOptionType(optionType);
            scope.setOptionCode(code);
            scope.setOptionLabel(labelMap.getOrDefault(code, code));
            scope.setSortOrder(index + 1);
            scopeMapper.insert(scope);
        }
    }

    private void persistSingleScopeItem(Long templateId, String optionType, String code, Map<String, String> labelMap) {
        String normalizedCode = owner.trimToNull(code);
        if (normalizedCode == null) {
            return;
        }
        ProcessTemplateScope scope = new ProcessTemplateScope();
        scope.setTemplateId(templateId);
        scope.setOptionType(optionType);
        scope.setOptionCode(normalizedCode);
        scope.setOptionLabel(labelMap.getOrDefault(normalizedCode, normalizedCode));
        scope.setSortOrder(1);
        scopeMapper.insert(scope);
    }

    private void persistAmountScopeItems(Long templateId, BigDecimal amountMin, BigDecimal amountMax) {
        if (amountMin != null) {
            saveSingleScopeValue(templateId, AbstractProcessManagementSupport.SCOPE_TYPE_AMOUNT_MIN, amountMin.stripTrailingZeros().toPlainString(), "最小金额", 1);
        }
        if (amountMax != null) {
            saveSingleScopeValue(templateId, AbstractProcessManagementSupport.SCOPE_TYPE_AMOUNT_MAX, amountMax.stripTrailingZeros().toPlainString(), "最大金额", 2);
        }
    }

    private void saveSingleScopeValue(Long templateId, String optionType, String optionCode, String optionLabel, int sortOrder) {
        ProcessTemplateScope scope = new ProcessTemplateScope();
        scope.setTemplateId(templateId);
        scope.setOptionType(optionType);
        scope.setOptionCode(optionCode);
        scope.setOptionLabel(optionLabel);
        scope.setSortOrder(sortOrder);
        scopeMapper.insert(scope);
    }

    private ProcessFormOptionVO option(String label, String value) {
        ProcessFormOptionVO option = new ProcessFormOptionVO();
        option.setLabel(label);
        option.setValue(value);
        return option;
    }
}
