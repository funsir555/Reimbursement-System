package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessCenterNavItemVO;
import com.finex.auth.dto.ProcessCenterOverviewVO;
import com.finex.auth.dto.ProcessCenterSummaryVO;
import com.finex.auth.dto.ProcessCustomArchiveDetailVO;
import com.finex.auth.dto.ProcessCustomArchiveItemDTO;
import com.finex.auth.dto.ProcessCustomArchiveMetaVO;
import com.finex.auth.dto.ProcessCustomArchiveOperatorVO;
import com.finex.auth.dto.ProcessCustomArchiveResolveDTO;
import com.finex.auth.dto.ProcessCustomArchiveResolveItemVO;
import com.finex.auth.dto.ProcessCustomArchiveResolveResultVO;
import com.finex.auth.dto.ProcessCustomArchiveRuleDTO;
import com.finex.auth.dto.ProcessCustomArchiveRuleFieldVO;
import com.finex.auth.dto.ProcessCustomArchiveSaveDTO;
import com.finex.auth.dto.ProcessCustomArchiveSummaryVO;
import com.finex.auth.dto.ProcessExpenseTypeConfigOptionVO;
import com.finex.auth.dto.ProcessExpenseTypeDetailVO;
import com.finex.auth.dto.ProcessExpenseTypeMetaVO;
import com.finex.auth.dto.ProcessExpenseTypeSaveDTO;
import com.finex.auth.dto.ProcessExpenseTypeTreeVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.dto.ProcessTemplateCardVO;
import com.finex.auth.dto.ProcessTemplateCategoryVO;
import com.finex.auth.dto.ProcessTemplateFormOptionsVO;
import com.finex.auth.dto.ProcessTemplateSaveDTO;
import com.finex.auth.dto.ProcessTemplateSaveResultVO;
import com.finex.auth.dto.ProcessTemplateTypeVO;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessCustomArchiveItem;
import com.finex.auth.entity.ProcessCustomArchiveRule;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseType;
import com.finex.auth.entity.ProcessTemplateCategory;
import com.finex.auth.entity.ProcessTemplateScope;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessCustomArchiveItemMapper;
import com.finex.auth.mapper.ProcessCustomArchiveRuleMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessTemplateCategoryMapper;
import com.finex.auth.mapper.ProcessTemplateScopeMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.ProcessManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcessManagementServiceImpl implements ProcessManagementService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String HIGHLIGHT_SEPARATOR = "|";

    private static final String DEFAULT_NUMBERING_RULE_CODE = "FX_DATE_4SEQ";
    private static final String DEFAULT_NUMBERING_RULE_PREVIEW = "FX+年+月+日+4位数字（如：FX202503251234）";

    private static final String ARCHIVE_TYPE_SELECT = "SELECT";
    private static final String ARCHIVE_TYPE_AUTO_RULE = "AUTO_RULE";
    private static final String CUSTOM_ARCHIVE_CODE_PREFIX = "CA";
    private static final String CUSTOM_ARCHIVE_ITEM_CODE_PREFIX = "CI";
    private static final String DEFAULT_TAG_ARCHIVE_CODE = "PROCESS_TAG_OPTIONS";
    private static final String DEFAULT_INSTALLMENT_ARCHIVE_CODE = "PROCESS_INSTALLMENT_OPTIONS";

    private static final String FIELD_VALUE_TYPE_TEXT = "text";
    private static final String FIELD_VALUE_TYPE_NUMBER = "number";
    private static final String FIELD_VALUE_TYPE_DEPARTMENT = "department";

    private static final String EXPENSE_TYPE_INVOICE_FREE = "FREE";
    private static final String EXPENSE_TYPE_INVOICE_REQUIRED = "NOT_FREE";
    private static final String EXPENSE_TYPE_TAX_DEFAULT = "DEFAULT";
    private static final String EXPENSE_TYPE_TAX_SPECIAL_NO_DEDUCT_NEED_OUT = "SPECIAL_NO_DEDUCT_NEED_OUT";
    private static final String EXPENSE_TYPE_TAX_SPECIAL_NO_DEDUCT_NEED_OUT_OTHERS_NONE = "SPECIAL_NO_DEDUCT_NEED_OUT_OTHERS_NONE";
    private static final String EXPENSE_TYPE_TAX_ALL_NO_DEDUCT_NO_OUT = "ALL_NO_DEDUCT_NO_OUT";
    private static final String EXPENSE_TYPE_TAX_HAS_DEDUCT_NO_DEDUCT_NEED_OUT = "HAS_DEDUCT_NO_DEDUCT_NEED_OUT";
    private static final String EXPENSE_TYPE_TAX_SEPARATE = "SEPARATE";
    private static final String EXPENSE_TYPE_TAX_NOT_SEPARATE = "NOT_SEPARATE";

    private static final Set<String> EXPENSE_TYPE_INVOICE_MODES = Set.of(
            EXPENSE_TYPE_INVOICE_FREE,
            EXPENSE_TYPE_INVOICE_REQUIRED
    );
    private static final Set<String> EXPENSE_TYPE_TAX_MODES = Set.of(
            EXPENSE_TYPE_TAX_DEFAULT,
            EXPENSE_TYPE_TAX_SPECIAL_NO_DEDUCT_NEED_OUT,
            EXPENSE_TYPE_TAX_SPECIAL_NO_DEDUCT_NEED_OUT_OTHERS_NONE,
            EXPENSE_TYPE_TAX_ALL_NO_DEDUCT_NO_OUT,
            EXPENSE_TYPE_TAX_HAS_DEDUCT_NO_DEDUCT_NEED_OUT
    );
    private static final Set<String> EXPENSE_TYPE_SEPARATION_MODES = Set.of(
            EXPENSE_TYPE_TAX_SEPARATE,
            EXPENSE_TYPE_TAX_NOT_SEPARATE
    );

    private static final List<String> OPERATOR_KEYS = List.of(
            "EQ",
            "NE",
            "IN",
            "NOT_IN",
            "GT",
            "GE",
            "LT",
            "LE",
            "BETWEEN",
            "CONTAINS"
    );

    private static final Map<String, String> OPERATOR_LABELS = Map.ofEntries(
            Map.entry("EQ", "等于"),
            Map.entry("NE", "不等于"),
            Map.entry("IN", "属于"),
            Map.entry("NOT_IN", "不属于"),
            Map.entry("GT", "大于"),
            Map.entry("GE", "大于等于"),
            Map.entry("LT", "小于"),
            Map.entry("LE", "小于等于"),
            Map.entry("BETWEEN", "介于"),
            Map.entry("CONTAINS", "包含")
    );

    private static final List<RuleFieldDefinition> RULE_FIELD_DEFINITIONS = List.of(
            new RuleFieldDefinition("submitterDeptId", "提单人部门", FIELD_VALUE_TYPE_DEPARTMENT, List.of("EQ", "NE", "IN", "NOT_IN")),
            new RuleFieldDefinition("submitterPosition", "提单人岗位", FIELD_VALUE_TYPE_TEXT, List.of("EQ", "NE", "IN", "NOT_IN", "CONTAINS")),
            new RuleFieldDefinition("laborRelationBelong", "劳动关系归属", FIELD_VALUE_TYPE_TEXT, List.of("EQ", "NE", "IN", "NOT_IN", "CONTAINS")),
            new RuleFieldDefinition("documentType", "单据类型", FIELD_VALUE_TYPE_TEXT, List.of("EQ", "NE", "IN", "NOT_IN")),
            new RuleFieldDefinition("amount", "金额", FIELD_VALUE_TYPE_NUMBER, List.of("EQ", "NE", "GT", "GE", "LT", "LE", "BETWEEN"))
    );

    private static final Map<String, RuleFieldDefinition> RULE_FIELD_MAP = RULE_FIELD_DEFINITIONS.stream()
            .collect(Collectors.toMap(RuleFieldDefinition::key, Function.identity()));

    private final ProcessTemplateCategoryMapper categoryMapper;
    private final ProcessDocumentTemplateMapper templateMapper;
    private final ProcessTemplateScopeMapper scopeMapper;
    private final ProcessCustomArchiveDesignMapper customArchiveDesignMapper;
    private final ProcessCustomArchiveItemMapper customArchiveItemMapper;
    private final ProcessCustomArchiveRuleMapper customArchiveRuleMapper;
    private final ProcessExpenseTypeMapper processExpenseTypeMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public ProcessCenterOverviewVO getOverview() {
        List<ProcessTemplateCategory> categories = categoryMapper.selectList(
                Wrappers.<ProcessTemplateCategory>lambdaQuery()
                        .eq(ProcessTemplateCategory::getStatus, 1)
                        .orderByAsc(ProcessTemplateCategory::getSortOrder, ProcessTemplateCategory::getId)
        );
        List<ProcessDocumentTemplate> templates = templateMapper.selectList(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .orderByAsc(ProcessDocumentTemplate::getSortOrder, ProcessDocumentTemplate::getId)
        );

        Map<String, ProcessTemplateCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(
                        ProcessTemplateCategory::getCategoryCode,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        ProcessCenterOverviewVO overview = new ProcessCenterOverviewVO();
        overview.setNavItems(buildNavItems());
        overview.setSummary(buildSummary(templates));
        overview.setCategories(buildCategoryCards(categories, templates, categoryMap));
        return overview;
    }

    @Override
    public List<ProcessTemplateTypeVO> getTemplateTypes() {
        return List.of(
                templateType("report", "报销单", "费用报销", "适用于员工报销、差旅报销与团队费用归集等场景。", "blue"),
                templateType("application", "申请单", "业务申请", "适用于预算申请、付款申请、项目申请等事前流程。", "cyan"),
                templateType("loan", "借款单", "借支管理", "适用于备用金借支、项目借款及后续核销归还场景。", "orange")
        );
    }

    @Override
    public ProcessTemplateFormOptionsVO getFormOptions(String templateType) {
        ProcessTemplateFormOptionsVO options = new ProcessTemplateFormOptionsVO();
        options.setTemplateType(templateType);
        options.setTemplateTypeLabel(resolveTemplateTypeLabel(templateType));
        options.setCategoryOptions(loadTemplateCategoryOptions());
        options.setNumberingRulePreview(DEFAULT_NUMBERING_RULE_PREVIEW);
        options.setPrintModes(List.of(
                option("默认打印模板", "default-print"),
                option("横版摘要模板", "landscape-summary"),
                option("财务归档模板", "finance-archive")
        ));
        options.setApprovalFlows(List.of(
                option("标准报销流程", "normal-expense-flow"),
                option("对公付款流程", "public-payment-flow"),
                option("借款与归还流程", "loan-return-flow")
        ));
        options.setPaymentModes(List.of(
                option("不生成付款单", "none"),
                option("生成对私付款单", "private-payment"),
                option("生成对公付款单", "public-payment")
        ));
        options.setAllocationForms(List.of(
                option("默认分摊表", "allocation-default"),
                option("项目分摊表", "allocation-project"),
                option("部门分摊表", "allocation-department")
        ));
        options.setExpenseTypes(loadEnabledExpenseTypeTree());
        options.setAiAuditModes(List.of(
                option("关闭 AI 审核", "disabled"),
                option("标准风险识别", "standard"),
                option("严格风险识别", "strict")
        ));
        options.setScopeOptions(List.of(
                option("限定部门使用", "department"),
                option("限定岗位使用", "position"),
                option("限定费用类型使用", "expense-type"),
                option("限定金额区间", "amount-range")
        ));
        options.setTagOptions(loadSelectArchiveOptions(DEFAULT_TAG_ARCHIVE_CODE));
        options.setInstallmentOptions(loadSelectArchiveOptions(DEFAULT_INSTALLMENT_ARCHIVE_CODE));
        return options;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessTemplateSaveResultVO saveTemplate(ProcessTemplateSaveDTO dto, String operatorName) {
        String categoryCode = normalize(dto.getCategory(), "employee-expense");
        String templateType = normalize(dto.getTemplateType(), "report");
        boolean enabled = dto.getEnabled() == null || dto.getEnabled();

        Map<String, String> tagLabelMap = selectArchiveLabelMap(DEFAULT_TAG_ARCHIVE_CODE);
        Map<String, String> installmentLabelMap = selectArchiveLabelMap(DEFAULT_INSTALLMENT_ARCHIVE_CODE);

        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateCode(buildTemplateCode());
        template.setTemplateName(trimToEmpty(dto.getTemplateName()));
        template.setTemplateType(templateType);
        template.setTemplateTypeLabel(resolveTemplateTypeLabel(templateType));
        template.setCategoryCode(categoryCode);
        template.setTemplateDescription(resolveDescription(dto));
        template.setNumberingRule(DEFAULT_NUMBERING_RULE_CODE);
        template.setIconColor(normalize(dto.getIconColor(), "blue"));
        template.setEnabled(enabled ? 1 : 0);
        template.setPublishStatus(enabled ? "ENABLED" : "DRAFT");
        template.setPrintMode(normalize(dto.getPrintMode(), "default-print"));
        template.setApprovalFlow(normalize(dto.getApprovalFlow(), "normal-expense-flow"));
        template.setFlowName(resolveFlowLabel(template.getApprovalFlow()));
        template.setPaymentMode(normalize(dto.getPaymentMode(), "none"));
        template.setAllocationForm(normalize(dto.getAllocationForm(), "allocation-default"));
        template.setAiAuditMode(normalize(dto.getAiAuditMode(), "disabled"));
        template.setHighlights(String.join(HIGHLIGHT_SEPARATOR, buildHighlights(dto, tagLabelMap, installmentLabelMap)));
        template.setOwnerName(normalize(operatorName, "流程管理员"));
        template.setSortOrder(nextSortOrder(categoryCode));
        templateMapper.insert(template);

        saveScopeItems(template.getId(), "EXPENSE_TYPE", dto.getExpenseTypes(), expenseTypeLabelMap());
        saveScopeItems(template.getId(), "SCOPE_OPTION", dto.getScopeOptions(), scopeLabelMap());
        saveSingleScopeItem(template.getId(), "TAG_OPTION", dto.getTagOption(), tagLabelMap);
        saveSingleScopeItem(template.getId(), "INSTALLMENT_OPTION", dto.getInstallmentOption(), installmentLabelMap);

        ProcessTemplateSaveResultVO result = new ProcessTemplateSaveResultVO();
        result.setId(template.getId());
        result.setTemplateCode(template.getTemplateCode());
        result.setTemplateName(template.getTemplateName());
        result.setStatus(template.getPublishStatus());
        return result;
    }

    @Override
    public List<ProcessCustomArchiveSummaryVO> listCustomArchives() {
        List<ProcessCustomArchiveDesign> archives = customArchiveDesignMapper.selectList(
                Wrappers.<ProcessCustomArchiveDesign>lambdaQuery()
                        .orderByDesc(ProcessCustomArchiveDesign::getCreatedAt, ProcessCustomArchiveDesign::getId)
        );
        if (archives.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> archiveIds = archives.stream().map(ProcessCustomArchiveDesign::getId).toList();
        Map<Long, Long> itemCountMap = customArchiveItemMapper.selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .in(ProcessCustomArchiveItem::getArchiveId, archiveIds)
        ).stream().collect(Collectors.groupingBy(ProcessCustomArchiveItem::getArchiveId, Collectors.counting()));

        return archives.stream().map(archive -> {
            ProcessCustomArchiveSummaryVO summary = new ProcessCustomArchiveSummaryVO();
            summary.setId(archive.getId());
            summary.setArchiveCode(archive.getArchiveCode());
            summary.setArchiveName(archive.getArchiveName());
            summary.setArchiveType(archive.getArchiveType());
            summary.setArchiveTypeLabel(resolveArchiveTypeLabel(archive.getArchiveType()));
            summary.setArchiveDescription(archive.getArchiveDescription());
            summary.setStatus(archive.getStatus());
            summary.setItemCount(itemCountMap.getOrDefault(archive.getId(), 0L).intValue());
            summary.setUpdatedAt(formatDateTime(archive.getUpdatedAt()));
            return summary;
        }).toList();
    }

    @Override
    public ProcessCustomArchiveDetailVO getCustomArchiveDetail(Long id) {
        return buildCustomArchiveDetail(requireCustomArchive(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessCustomArchiveDetailVO createCustomArchive(ProcessCustomArchiveSaveDTO dto) {
        validateCustomArchive(dto);

        ProcessCustomArchiveDesign archive = new ProcessCustomArchiveDesign();
        applyCustomArchiveBase(archive, dto);
        archive.setArchiveCode(buildCustomArchiveCode());
        customArchiveDesignMapper.insert(archive);

        replaceCustomArchiveItems(archive.getId(), dto);
        return buildCustomArchiveDetail(requireCustomArchive(archive.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessCustomArchiveDetailVO updateCustomArchive(Long id, ProcessCustomArchiveSaveDTO dto) {
        ProcessCustomArchiveDesign archive = requireCustomArchive(id);
        validateCustomArchive(dto);

        applyCustomArchiveBase(archive, dto);
        customArchiveDesignMapper.updateById(archive);
        replaceCustomArchiveItems(id, dto);
        return buildCustomArchiveDetail(requireCustomArchive(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCustomArchiveStatus(Long id, Integer status) {
        ProcessCustomArchiveDesign archive = requireCustomArchive(id);
        archive.setStatus(normalizeStatus(status));
        customArchiveDesignMapper.updateById(archive);
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCustomArchive(Long id) {
        ProcessCustomArchiveDesign archive = requireCustomArchive(id);
        List<ProcessCustomArchiveItem> items = customArchiveItemMapper.selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .eq(ProcessCustomArchiveItem::getArchiveId, id)
        );
        List<String> itemCodes = items.stream()
                .map(ProcessCustomArchiveItem::getItemCode)
                .filter(Objects::nonNull)
                .toList();
        if (!itemCodes.isEmpty()) {
            Long referencedCount = scopeMapper.selectCount(
                    Wrappers.<ProcessTemplateScope>lambdaQuery()
                            .in(ProcessTemplateScope::getOptionType, List.of("TAG_OPTION", "INSTALLMENT_OPTION"))
                            .in(ProcessTemplateScope::getOptionCode, itemCodes)
            );
            if (referencedCount != null && referencedCount > 0) {
                throw new IllegalStateException("当前档案结果已被模板引用，无法删除");
            }
        }

        deleteArchiveChildren(id);
        customArchiveDesignMapper.deleteById(archive.getId());
        return Boolean.TRUE;
    }

    @Override
    public ProcessCustomArchiveMetaVO getCustomArchiveMeta() {
        ProcessCustomArchiveMetaVO meta = new ProcessCustomArchiveMetaVO();
        meta.setArchiveTypeOptions(List.of(
                option("提供选择", ARCHIVE_TYPE_SELECT),
                option("自动划分", ARCHIVE_TYPE_AUTO_RULE)
        ));
        meta.setOperatorOptions(OPERATOR_KEYS.stream().map(key -> {
            ProcessCustomArchiveOperatorVO operator = new ProcessCustomArchiveOperatorVO();
            operator.setKey(key);
            operator.setLabel(OPERATOR_LABELS.getOrDefault(key, key));
            return operator;
        }).toList());
        meta.setRuleFields(RULE_FIELD_DEFINITIONS.stream().map(definition -> {
            ProcessCustomArchiveRuleFieldVO field = new ProcessCustomArchiveRuleFieldVO();
            field.setKey(definition.key());
            field.setLabel(definition.label());
            field.setValueType(definition.valueType());
            field.setOperatorKeys(new ArrayList<>(definition.operatorKeys()));
            return field;
        }).toList());
        meta.setDepartmentOptions(loadDepartmentOptions());
        meta.setTagArchiveCode(DEFAULT_TAG_ARCHIVE_CODE);
        meta.setInstallmentArchiveCode(DEFAULT_INSTALLMENT_ARCHIVE_CODE);
        return meta;
    }

    @Override
    public ProcessCustomArchiveResolveResultVO resolveCustomArchive(ProcessCustomArchiveResolveDTO dto) {
        ProcessCustomArchiveDesign archive = requireCustomArchive(trimToEmpty(dto.getArchiveCode()));

        ProcessCustomArchiveResolveResultVO result = new ProcessCustomArchiveResolveResultVO();
        result.setArchiveCode(archive.getArchiveCode());
        result.setArchiveType(archive.getArchiveType());

        if (ARCHIVE_TYPE_SELECT.equals(archive.getArchiveType())) {
            result.setItems(resolveSelectArchive(archive.getId()));
            return result;
        }

        result.setItems(resolveAutoRuleArchive(archive.getId(), dto.getContext()));
        return result;
    }

    @Override
    public List<ProcessExpenseTypeTreeVO> listExpenseTypeTree() {
        return buildExpenseTypeTree(loadAllExpenseTypes());
    }

    @Override
    public ProcessExpenseTypeMetaVO getExpenseTypeMeta() {
        ProcessExpenseTypeMetaVO meta = new ProcessExpenseTypeMetaVO();
        meta.setDepartmentOptions(loadDepartmentOptions());
        meta.setUserOptions(loadUserOptions());
        meta.setInvoiceFreeOptions(List.of(
                configOption(EXPENSE_TYPE_INVOICE_FREE, "免票", "默认无需上传发票，且费用自动标记为免票"),
                configOption(EXPENSE_TYPE_INVOICE_REQUIRED, "不免票", "根据费用表单中发票组件的必填性进行判断")
        ));
        meta.setTaxDeductionOptions(List.of(
                configOption(EXPENSE_TYPE_TAX_DEFAULT, "遵循默认抵扣和转出逻辑", "本费用类型下继续使用系统默认的发票抵扣与转出规则"),
                configOption(EXPENSE_TYPE_TAX_SPECIAL_NO_DEDUCT_NEED_OUT, "专票不抵扣且需要转出", "上传专票时默认不抵扣且需要转出，其余票种遵循默认抵扣转出逻辑"),
                configOption(EXPENSE_TYPE_TAX_SPECIAL_NO_DEDUCT_NEED_OUT_OTHERS_NONE, "专票转出，其余票种不抵扣不转出", "上传专票时默认不抵扣且需要转出；上传其余发票不抵扣不转出"),
                configOption(EXPENSE_TYPE_TAX_ALL_NO_DEDUCT_NO_OUT, "任何票种都不抵扣且无需转出", "无论上传任何票种，默认不抵扣，且无需转出"),
                configOption(EXPENSE_TYPE_TAX_HAS_DEDUCT_NO_DEDUCT_NEED_OUT, "有抵扣税额时不抵扣且需转出", "上传有抵扣税额的发票时，默认不抵扣，且需要转出")
        ));
        meta.setTaxSeparationOptions(List.of(
                configOption(EXPENSE_TYPE_TAX_SEPARATE, "价税分离", "费用录入时金额和税额分开展示"),
                configOption(EXPENSE_TYPE_TAX_NOT_SEPARATE, "价税不分离", "费用录入时按含税金额处理")
        ));
        return meta;
    }

    @Override
    public ProcessExpenseTypeDetailVO getExpenseTypeDetail(Long id) {
        return buildExpenseTypeDetail(requireExpenseType(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessExpenseTypeDetailVO createExpenseType(ProcessExpenseTypeSaveDTO dto) {
        validateExpenseType(dto, null);

        ProcessExpenseType expenseType = new ProcessExpenseType();
        applyExpenseTypeBase(expenseType, dto);
        processExpenseTypeMapper.insert(expenseType);
        return buildExpenseTypeDetail(requireExpenseType(expenseType.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessExpenseTypeDetailVO updateExpenseType(Long id, ProcessExpenseTypeSaveDTO dto) {
        ProcessExpenseType expenseType = requireExpenseType(id);
        validateExpenseType(dto, expenseType);

        Integer targetStatus = normalizeStatus(dto.getStatus());
        applyExpenseTypeBase(expenseType, dto);
        processExpenseTypeMapper.updateById(expenseType);
        if (targetStatus == 0) {
            disableExpenseTypeChildren(id);
        }
        return buildExpenseTypeDetail(requireExpenseType(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateExpenseTypeStatus(Long id, Integer status) {
        ProcessExpenseType expenseType = requireExpenseType(id);
        Integer normalizedStatus = normalizeStatus(status);
        validateExpenseTypeStatus(expenseType, normalizedStatus);

        expenseType.setStatus(normalizedStatus);
        processExpenseTypeMapper.updateById(expenseType);
        if (normalizedStatus == 0) {
            disableExpenseTypeChildren(id);
        }
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteExpenseType(Long id) {
        ProcessExpenseType expenseType = requireExpenseType(id);
        if (hasExpenseTypeChildren(id)) {
            throw new IllegalStateException("当前费用类型存在下级节点，无法删除");
        }
        if (isExpenseTypeReferenced(expenseType)) {
            throw new IllegalStateException("当前费用类型已被模板引用，无法删除");
        }
        processExpenseTypeMapper.deleteById(id);
        return Boolean.TRUE;
    }
    private List<ProcessCenterNavItemVO> buildNavItems() {
        return List.of(
                navItem("document-flow", "单据与流程", "统一维护模板、审批流和单据能力"),
                navItem("custom-archive", "自定义档案", "维护标签档案、分期付款档案和自动划分规则"),
                navItem("expense-type", "费用类型", "维护费用类型树及发票税务配置")
        );
    }

    private ProcessCenterSummaryVO buildSummary(List<ProcessDocumentTemplate> templates) {
        ProcessCenterSummaryVO summary = new ProcessCenterSummaryVO();
        summary.setTotalTemplates(templates.size());
        summary.setEnabledTemplates((int) templates.stream().filter(item -> Objects.equals(item.getEnabled(), 1)).count());
        summary.setDraftTemplates((int) templates.stream().filter(item -> !"ENABLED".equals(item.getPublishStatus())).count());
        summary.setAiAuditTemplates((int) templates.stream()
                .filter(item -> !"disabled".equalsIgnoreCase(normalize(item.getAiAuditMode(), "disabled")))
                .count());
        return summary;
    }

    private List<ProcessTemplateCategoryVO> buildCategoryCards(
            List<ProcessTemplateCategory> categories,
            List<ProcessDocumentTemplate> templates,
            Map<String, ProcessTemplateCategory> categoryMap
    ) {
        Map<String, List<ProcessDocumentTemplate>> groupedTemplates = templates.stream()
                .collect(Collectors.groupingBy(
                        item -> normalize(item.getCategoryCode(), "uncategorized"),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<ProcessTemplateCategoryVO> result = new ArrayList<>();
        for (ProcessTemplateCategory category : categories) {
            List<ProcessDocumentTemplate> categoryTemplates = groupedTemplates.getOrDefault(category.getCategoryCode(), Collections.emptyList());
            ProcessTemplateCategoryVO card = new ProcessTemplateCategoryVO();
            card.setCode(category.getCategoryCode());
            card.setName(category.getCategoryName());
            card.setDescription(normalize(category.getCategoryDescription(), "暂无说明"));
            card.setTemplateCount(categoryTemplates.size());
            card.setTemplates(buildTemplateCards(categoryTemplates, category.getCategoryName()));
            result.add(card);
        }

        for (Map.Entry<String, List<ProcessDocumentTemplate>> entry : groupedTemplates.entrySet()) {
            if (categoryMap.containsKey(entry.getKey())) {
                continue;
            }
            ProcessTemplateCategoryVO card = new ProcessTemplateCategoryVO();
            card.setCode(entry.getKey());
            card.setName(entry.getKey());
            card.setDescription("未分组模板");
            card.setTemplateCount(entry.getValue().size());
            card.setTemplates(buildTemplateCards(entry.getValue(), entry.getKey()));
            result.add(card);
        }
        return result;
    }

    private List<ProcessTemplateCardVO> buildTemplateCards(List<ProcessDocumentTemplate> templates, String categoryName) {
        return templates.stream().map(template -> {
            ProcessTemplateCardVO card = new ProcessTemplateCardVO();
            card.setId(template.getId());
            card.setTemplateCode(template.getTemplateCode());
            card.setName(template.getTemplateName());
            card.setTemplateType(normalize(template.getTemplateTypeLabel(), resolveTemplateTypeLabel(template.getTemplateType())));
            card.setBusinessDomain(categoryName);
            card.setDescription(normalize(template.getTemplateDescription(), "暂无说明"));
            card.setHighlights(splitHighlights(template.getHighlights()));
            card.setFlowName(normalize(template.getFlowName(), "标准审批流程"));
            card.setUpdatedAt(formatDateTime(template.getUpdatedAt()));
            card.setOwner(normalize(template.getOwnerName(), "流程管理员"));
            card.setColor(resolveColor(template.getIconColor()));
            return card;
        }).toList();
    }

    private List<String> splitHighlights(String highlights) {
        if (trimToNull(highlights) == null) {
            return List.of("标准审批链路");
        }
        return List.of(highlights.split("\\|")).stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private List<ProcessFormOptionVO> loadTemplateCategoryOptions() {
        List<ProcessFormOptionVO> options = categoryMapper.selectList(
                Wrappers.<ProcessTemplateCategory>lambdaQuery()
                        .eq(ProcessTemplateCategory::getStatus, 1)
                        .orderByAsc(ProcessTemplateCategory::getSortOrder, ProcessTemplateCategory::getId)
        ).stream().map(category -> option(category.getCategoryName(), category.getCategoryCode())).toList();

        if (!options.isEmpty()) {
            return options;
        }
        return List.of(
                option("员工费用类", "employee-expense"),
                option("企业往来类", "enterprise-payment"),
                option("事项申请类", "business-application")
        );
    }

    private List<String> buildHighlights(
            ProcessTemplateSaveDTO dto,
            Map<String, String> tagLabelMap,
            Map<String, String> installmentLabelMap
    ) {
        LinkedHashSet<String> highlights = new LinkedHashSet<>();
        highlights.add("支持移动端提单");
        if (!"none".equalsIgnoreCase(normalize(dto.getPaymentMode(), "none"))) {
            highlights.add("联动付款单");
        }
        if (!"disabled".equalsIgnoreCase(normalize(dto.getAiAuditMode(), "disabled"))) {
            highlights.add("AI 审核");
        }

        String tagLabel = tagLabelMap.get(trimToEmpty(dto.getTagOption()));
        if (tagLabel != null) {
            highlights.add(tagLabel);
        }
        String installmentLabel = installmentLabelMap.get(trimToEmpty(dto.getInstallmentOption()));
        if (installmentLabel != null) {
            highlights.add(installmentLabel);
        }

        while (highlights.size() < 3) {
            highlights.add("标准审批链路");
        }
        return highlights.stream().limit(3).toList();
    }

    private void saveScopeItems(Long templateId, String optionType, List<String> codes, Map<String, String> labelMap) {
        List<String> normalizedCodes = normalizeIdList(codes);
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

    private void saveSingleScopeItem(Long templateId, String optionType, String code, Map<String, String> labelMap) {
        String normalizedCode = trimToNull(code);
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

    private int nextSortOrder(String categoryCode) {
        List<ProcessDocumentTemplate> templates = templateMapper.selectList(
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

    private String resolveDescription(ProcessTemplateSaveDTO dto) {
        String description = trimToNull(dto.getTemplateDescription());
        if (description != null) {
            return description;
        }
        return resolveTemplateTypeLabel(dto.getTemplateType()) + "模板";
    }

    private String resolveTemplateTypeLabel(String templateType) {
        return switch (normalize(templateType, "report")) {
            case "application" -> "申请单";
            case "loan" -> "借款单";
            default -> "报销单";
        };
    }

    private String resolveFlowLabel(String approvalFlow) {
        return switch (normalize(approvalFlow, "normal-expense-flow")) {
            case "public-payment-flow" -> "对公付款流程";
            case "loan-return-flow" -> "借款与归还流程";
            default -> "标准报销流程";
        };
    }

    private String resolveColor(String iconColor) {
        return switch (normalize(iconColor, "blue")) {
            case "cyan" -> "linear-gradient(135deg, #0891b2 0%, #67e8f9 100%)";
            case "orange" -> "linear-gradient(135deg, #ea580c 0%, #fdba74 100%)";
            case "green" -> "linear-gradient(135deg, #15803d 0%, #86efac 100%)";
            default -> "linear-gradient(135deg, #2563eb 0%, #60a5fa 100%)";
        };
    }

    private String buildTemplateCode() {
        String prefix = "FX" + LocalDate.now().format(CODE_DATE_FORMATTER);
        Long count = templateMapper.selectCount(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .likeRight(ProcessDocumentTemplate::getTemplateCode, prefix)
        );
        long next = count == null ? 1L : count + 1L;
        return prefix + String.format("%04d", next);
    }

    private Map<String, String> expenseTypeLabelMap() {
        return loadAllExpenseTypes().stream().collect(Collectors.toMap(
                ProcessExpenseType::getExpenseCode,
                ProcessExpenseType::getExpenseName,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private Map<String, String> scopeLabelMap() {
        Map<String, String> labelMap = new LinkedHashMap<>();
        labelMap.put("department", "限定部门使用");
        labelMap.put("position", "限定岗位使用");
        labelMap.put("expense-type", "限定费用类型使用");
        labelMap.put("amount-range", "限定金额区间");
        return labelMap;
    }

    private List<ProcessFormOptionVO> loadSelectArchiveOptions(String archiveCode) {
        ProcessCustomArchiveDesign archive = customArchiveDesignMapper.selectOne(
                Wrappers.<ProcessCustomArchiveDesign>lambdaQuery()
                        .eq(ProcessCustomArchiveDesign::getArchiveCode, archiveCode)
                        .eq(ProcessCustomArchiveDesign::getStatus, 1)
                        .last("limit 1")
        );
        if (archive == null || !ARCHIVE_TYPE_SELECT.equals(archive.getArchiveType())) {
            return Collections.emptyList();
        }

        return customArchiveItemMapper.selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .eq(ProcessCustomArchiveItem::getArchiveId, archive.getId())
                        .eq(ProcessCustomArchiveItem::getStatus, 1)
                        .orderByAsc(ProcessCustomArchiveItem::getId)
        ).stream().map(item -> option(item.getItemName(), item.getItemCode())).toList();
    }

    private Map<String, String> selectArchiveLabelMap(String archiveCode) {
        return loadSelectArchiveOptions(archiveCode).stream().collect(Collectors.toMap(
                ProcessFormOptionVO::getValue,
                ProcessFormOptionVO::getLabel,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }
    private ProcessCustomArchiveDesign requireCustomArchive(Long id) {
        ProcessCustomArchiveDesign archive = customArchiveDesignMapper.selectById(id);
        if (archive == null) {
            throw new IllegalStateException("未找到对应的自定义档案");
        }
        return archive;
    }

    private ProcessCustomArchiveDesign requireCustomArchive(String archiveCode) {
        ProcessCustomArchiveDesign archive = customArchiveDesignMapper.selectOne(
                Wrappers.<ProcessCustomArchiveDesign>lambdaQuery()
                        .eq(ProcessCustomArchiveDesign::getArchiveCode, archiveCode)
                        .last("limit 1")
        );
        if (archive == null) {
            throw new IllegalStateException("未找到对应的自定义档案");
        }
        return archive;
    }

    private ProcessCustomArchiveDetailVO buildCustomArchiveDetail(ProcessCustomArchiveDesign archive) {
        ProcessCustomArchiveDetailVO detail = new ProcessCustomArchiveDetailVO();
        detail.setId(archive.getId());
        detail.setArchiveCode(archive.getArchiveCode());
        detail.setArchiveName(archive.getArchiveName());
        detail.setArchiveType(archive.getArchiveType());
        detail.setArchiveTypeLabel(resolveArchiveTypeLabel(archive.getArchiveType()));
        detail.setArchiveDescription(archive.getArchiveDescription());
        detail.setStatus(archive.getStatus());

        List<ProcessCustomArchiveItem> items = customArchiveItemMapper.selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .eq(ProcessCustomArchiveItem::getArchiveId, archive.getId())
        );
        items = sortArchiveItems(items, archive.getArchiveType());

        Map<Long, List<ProcessCustomArchiveRule>> ruleMap = loadRuleMap(
                items.stream().map(ProcessCustomArchiveItem::getId).toList()
        );
        detail.setItems(items.stream().map(item -> toArchiveItemDto(item, ruleMap.getOrDefault(item.getId(), Collections.emptyList()))).toList());
        return detail;
    }

    private ProcessCustomArchiveItemDTO toArchiveItemDto(ProcessCustomArchiveItem item, List<ProcessCustomArchiveRule> rules) {
        ProcessCustomArchiveItemDTO dto = new ProcessCustomArchiveItemDTO();
        dto.setId(item.getId());
        dto.setItemCode(item.getItemCode());
        dto.setItemName(item.getItemName());
        dto.setPriority(item.getPriority());
        dto.setStatus(item.getStatus());
        dto.setRules(rules.stream().map(this::toArchiveRuleDto).toList());
        return dto;
    }

    private ProcessCustomArchiveRuleDTO toArchiveRuleDto(ProcessCustomArchiveRule rule) {
        ProcessCustomArchiveRuleDTO dto = new ProcessCustomArchiveRuleDTO();
        dto.setId(rule.getId());
        dto.setGroupNo(rule.getGroupNo());
        dto.setFieldKey(rule.getFieldKey());
        dto.setOperator(rule.getOperator());
        dto.setCompareValue(deserializeCompareValue(rule.getCompareValue()));
        return dto;
    }

    private Map<Long, List<ProcessCustomArchiveRule>> loadRuleMap(List<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return customArchiveRuleMapper.selectList(
                Wrappers.<ProcessCustomArchiveRule>lambdaQuery()
                        .in(ProcessCustomArchiveRule::getArchiveItemId, itemIds)
                        .orderByAsc(ProcessCustomArchiveRule::getGroupNo, ProcessCustomArchiveRule::getId)
        ).stream().collect(Collectors.groupingBy(
                ProcessCustomArchiveRule::getArchiveItemId,
                LinkedHashMap::new,
                Collectors.toList()
        ));
    }

    private void applyCustomArchiveBase(ProcessCustomArchiveDesign archive, ProcessCustomArchiveSaveDTO dto) {
        archive.setArchiveName(trimToEmpty(dto.getArchiveName()));
        archive.setArchiveType(trimToEmpty(dto.getArchiveType()));
        archive.setArchiveDescription(trimToNull(dto.getArchiveDescription()));
        archive.setStatus(normalizeStatus(dto.getStatus()));
    }

    private void validateCustomArchive(ProcessCustomArchiveSaveDTO dto) {
        if (!Set.of(ARCHIVE_TYPE_SELECT, ARCHIVE_TYPE_AUTO_RULE).contains(trimToEmpty(dto.getArchiveType()))) {
            throw new IllegalArgumentException("自定义档案类型仅支持 SELECT 或 AUTO_RULE");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("请至少配置一个结果项");
        }

        for (ProcessCustomArchiveItemDTO item : dto.getItems()) {
            if (trimToNull(item.getItemName()) == null) {
                throw new IllegalArgumentException("结果项名称不能为空");
            }
            if (ARCHIVE_TYPE_AUTO_RULE.equals(dto.getArchiveType())) {
                validateRules(item.getRules());
            }
        }
    }

    private void validateRules(List<ProcessCustomArchiveRuleDTO> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("自动划分档案的每个结果项至少需要一条规则");
        }
        for (ProcessCustomArchiveRuleDTO rule : rules) {
            if (rule.getGroupNo() == null || rule.getGroupNo() < 1) {
                throw new IllegalArgumentException("规则组必须为大于 0 的整数");
            }
            RuleFieldDefinition definition = RULE_FIELD_MAP.get(rule.getFieldKey());
            if (definition == null) {
                throw new IllegalArgumentException("存在不支持的规则字段: " + rule.getFieldKey());
            }
            if (!definition.operatorKeys().contains(rule.getOperator())) {
                throw new IllegalArgumentException("字段 " + definition.label() + " 不支持操作符 " + rule.getOperator());
            }
            if (rule.getCompareValue() == null) {
                throw new IllegalArgumentException("规则比较值不能为空");
            }
            if ("BETWEEN".equals(rule.getOperator())) {
                Object compareValue = rule.getCompareValue();
                if (!(compareValue instanceof List<?> valueList) || valueList.size() < 2) {
                    throw new IllegalArgumentException("BETWEEN 操作符需要提供两个边界值");
                }
            }
        }
    }

    private void replaceCustomArchiveItems(Long archiveId, ProcessCustomArchiveSaveDTO dto) {
        deleteArchiveChildren(archiveId);

        for (int index = 0; index < dto.getItems().size(); index++) {
            ProcessCustomArchiveItemDTO itemDto = dto.getItems().get(index);
            ProcessCustomArchiveItem item = new ProcessCustomArchiveItem();
            item.setArchiveId(archiveId);
            item.setItemCode(resolveArchiveItemCode(itemDto));
            item.setItemName(trimToEmpty(itemDto.getItemName()));
            item.setPriority(itemDto.getPriority() == null ? index + 1 : itemDto.getPriority());
            item.setStatus(normalizeStatus(itemDto.getStatus()));
            customArchiveItemMapper.insert(item);

            if (!ARCHIVE_TYPE_AUTO_RULE.equals(dto.getArchiveType())) {
                continue;
            }

            for (ProcessCustomArchiveRuleDTO ruleDto : itemDto.getRules()) {
                ProcessCustomArchiveRule rule = new ProcessCustomArchiveRule();
                rule.setArchiveItemId(item.getId());
                rule.setGroupNo(ruleDto.getGroupNo());
                rule.setFieldKey(trimToEmpty(ruleDto.getFieldKey()));
                rule.setOperator(trimToEmpty(ruleDto.getOperator()));
                rule.setCompareValue(serializeCompareValue(ruleDto.getCompareValue()));
                customArchiveRuleMapper.insert(rule);
            }
        }
    }

    private String resolveArchiveItemCode(ProcessCustomArchiveItemDTO itemDto) {
        String itemCode = trimToNull(itemDto.getItemCode());
        return itemCode != null ? itemCode : buildCustomArchiveItemCode();
    }

    private void deleteArchiveChildren(Long archiveId) {
        List<ProcessCustomArchiveItem> items = customArchiveItemMapper.selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .eq(ProcessCustomArchiveItem::getArchiveId, archiveId)
        );
        if (!items.isEmpty()) {
            customArchiveRuleMapper.delete(
                    Wrappers.<ProcessCustomArchiveRule>lambdaQuery()
                            .in(ProcessCustomArchiveRule::getArchiveItemId, items.stream().map(ProcessCustomArchiveItem::getId).toList())
            );
        }
        customArchiveItemMapper.delete(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .eq(ProcessCustomArchiveItem::getArchiveId, archiveId)
        );
    }

    private List<ProcessCustomArchiveItem> sortArchiveItems(List<ProcessCustomArchiveItem> items, String archiveType) {
        Comparator<ProcessCustomArchiveItem> comparator;
        if (ARCHIVE_TYPE_AUTO_RULE.equals(archiveType)) {
            comparator = Comparator
                    .comparing(ProcessCustomArchiveItem::getPriority, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(ProcessCustomArchiveItem::getId, Comparator.nullsLast(Long::compareTo));
        } else {
            comparator = Comparator.comparing(ProcessCustomArchiveItem::getId, Comparator.nullsLast(Long::compareTo));
        }
        return items.stream().sorted(comparator).toList();
    }

    private String buildCustomArchiveCode() {
        String prefix = CUSTOM_ARCHIVE_CODE_PREFIX + LocalDate.now().format(CODE_DATE_FORMATTER);
        Long count = customArchiveDesignMapper.selectCount(
                Wrappers.<ProcessCustomArchiveDesign>lambdaQuery()
                        .likeRight(ProcessCustomArchiveDesign::getArchiveCode, prefix)
        );
        long next = count == null ? 1L : count + 1L;
        return prefix + String.format("%04d", next);
    }

    private String buildCustomArchiveItemCode() {
        String prefix = CUSTOM_ARCHIVE_ITEM_CODE_PREFIX + LocalDate.now().format(CODE_DATE_FORMATTER);
        Long count = customArchiveItemMapper.selectCount(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .likeRight(ProcessCustomArchiveItem::getItemCode, prefix)
        );
        long next = count == null ? 1L : count + 1L;
        return prefix + String.format("%04d", next);
    }

    private String serializeCompareValue(Object compareValue) {
        try {
            return objectMapper.writeValueAsString(compareValue);
        } catch (Exception ex) {
            throw new IllegalStateException("规则比较值序列化失败", ex);
        }
    }

    private Object deserializeCompareValue(String compareValue) {
        if (trimToNull(compareValue) == null) {
            return null;
        }
        try {
            return objectMapper.readValue(compareValue, Object.class);
        } catch (Exception ex) {
            throw new IllegalStateException("规则比较值反序列化失败", ex);
        }
    }

    private List<ProcessCustomArchiveResolveItemVO> resolveSelectArchive(Long archiveId) {
        return customArchiveItemMapper.selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .eq(ProcessCustomArchiveItem::getArchiveId, archiveId)
                        .eq(ProcessCustomArchiveItem::getStatus, 1)
                        .orderByAsc(ProcessCustomArchiveItem::getId)
        ).stream().map(this::toResolvedItem).toList();
    }

    private List<ProcessCustomArchiveResolveItemVO> resolveAutoRuleArchive(Long archiveId, Map<String, Object> context) {
        List<ProcessCustomArchiveItem> items = customArchiveItemMapper.selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .eq(ProcessCustomArchiveItem::getArchiveId, archiveId)
                        .eq(ProcessCustomArchiveItem::getStatus, 1)
                        .orderByAsc(ProcessCustomArchiveItem::getPriority, ProcessCustomArchiveItem::getId)
        );
        Map<Long, List<ProcessCustomArchiveRule>> ruleMap = loadRuleMap(items.stream().map(ProcessCustomArchiveItem::getId).toList());

        List<ProcessCustomArchiveResolveItemVO> resolvedItems = new ArrayList<>();
        for (ProcessCustomArchiveItem item : items) {
            if (matchesItem(ruleMap.getOrDefault(item.getId(), Collections.emptyList()), context)) {
                resolvedItems.add(toResolvedItem(item));
            }
        }
        return resolvedItems;
    }

    private boolean matchesItem(List<ProcessCustomArchiveRule> rules, Map<String, Object> context) {
        if (rules.isEmpty()) {
            return false;
        }
        Map<Integer, List<ProcessCustomArchiveRule>> groupedRules = rules.stream().collect(Collectors.groupingBy(
                ProcessCustomArchiveRule::getGroupNo,
                LinkedHashMap::new,
                Collectors.toList()
        ));
        for (List<ProcessCustomArchiveRule> groupRules : groupedRules.values()) {
            boolean allMatched = true;
            for (ProcessCustomArchiveRule rule : groupRules) {
                if (!matchesRule(rule, context)) {
                    allMatched = false;
                    break;
                }
            }
            if (allMatched) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesRule(ProcessCustomArchiveRule rule, Map<String, Object> context) {
        Object actualValue = context == null ? null : context.get(rule.getFieldKey());
        Object expectedValue = deserializeCompareValue(rule.getCompareValue());
        return switch (rule.getOperator()) {
            case "EQ" -> equalsComparable(actualValue, expectedValue);
            case "NE" -> !equalsComparable(actualValue, expectedValue);
            case "IN" -> collectionContains(expectedValue, actualValue);
            case "NOT_IN" -> !collectionContains(expectedValue, actualValue);
            case "GT" -> compareNumbers(actualValue, expectedValue) > 0;
            case "GE" -> compareNumbers(actualValue, expectedValue) >= 0;
            case "LT" -> compareNumbers(actualValue, expectedValue) < 0;
            case "LE" -> compareNumbers(actualValue, expectedValue) <= 0;
            case "BETWEEN" -> matchesBetween(actualValue, expectedValue);
            case "CONTAINS" -> normalizeComparable(actualValue).contains(normalizeComparable(expectedValue));
            default -> false;
        };
    }

    private boolean equalsComparable(Object actualValue, Object expectedValue) {
        BigDecimal actualNumber = toBigDecimal(actualValue);
        BigDecimal expectedNumber = toBigDecimal(expectedValue);
        if (actualNumber != null && expectedNumber != null) {
            return actualNumber.compareTo(expectedNumber) == 0;
        }
        return Objects.equals(normalizeComparable(actualValue), normalizeComparable(expectedValue));
    }

    private boolean collectionContains(Object collectionValue, Object actualValue) {
        if (collectionValue instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (equalsComparable(actualValue, item)) {
                    return true;
                }
            }
            return false;
        }
        return equalsComparable(actualValue, collectionValue);
    }

    private int compareNumbers(Object actualValue, Object expectedValue) {
        BigDecimal actualNumber = toBigDecimal(actualValue);
        BigDecimal expectedNumber = toBigDecimal(expectedValue);
        if (actualNumber == null || expectedNumber == null) {
            return -1;
        }
        return actualNumber.compareTo(expectedNumber);
    }

    private boolean matchesBetween(Object actualValue, Object expectedValue) {
        BigDecimal actualNumber = toBigDecimal(actualValue);
        if (actualNumber == null || !(expectedValue instanceof List<?> valueList) || valueList.size() < 2) {
            return false;
        }
        BigDecimal start = toBigDecimal(valueList.get(0));
        BigDecimal end = toBigDecimal(valueList.get(1));
        if (start == null || end == null) {
            return false;
        }
        return actualNumber.compareTo(start) >= 0 && actualNumber.compareTo(end) <= 0;
    }

    private ProcessCustomArchiveResolveItemVO toResolvedItem(ProcessCustomArchiveItem item) {
        ProcessCustomArchiveResolveItemVO resolvedItem = new ProcessCustomArchiveResolveItemVO();
        resolvedItem.setItemCode(item.getItemCode());
        resolvedItem.setItemName(item.getItemName());
        resolvedItem.setPriority(item.getPriority());
        return resolvedItem;
    }

    private String resolveArchiveTypeLabel(String archiveType) {
        if (ARCHIVE_TYPE_AUTO_RULE.equals(archiveType)) {
            return "自动划分";
        }
        return "提供选择";
    }
    private List<ProcessExpenseType> loadAllExpenseTypes() {
        return processExpenseTypeMapper.selectList(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .orderByAsc(ProcessExpenseType::getExpenseCode, ProcessExpenseType::getId)
        );
    }

    private List<ProcessExpenseType> loadEnabledExpenseTypes() {
        return processExpenseTypeMapper.selectList(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .eq(ProcessExpenseType::getStatus, 1)
                        .orderByAsc(ProcessExpenseType::getExpenseCode, ProcessExpenseType::getId)
        );
    }

    private List<ProcessExpenseTypeTreeVO> loadEnabledExpenseTypeTree() {
        return buildExpenseTypeTree(loadEnabledExpenseTypes());
    }

    private List<ProcessExpenseTypeTreeVO> buildExpenseTypeTree(List<ProcessExpenseType> expenseTypes) {
        if (expenseTypes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ProcessExpenseTypeTreeVO> nodeMap = new LinkedHashMap<>();
        List<ProcessExpenseTypeTreeVO> roots = new ArrayList<>();
        for (ProcessExpenseType expenseType : expenseTypes) {
            nodeMap.put(expenseType.getId(), toExpenseTypeTree(expenseType));
        }

        for (ProcessExpenseType expenseType : expenseTypes) {
            ProcessExpenseTypeTreeVO node = nodeMap.get(expenseType.getId());
            if (expenseType.getParentId() == null || !nodeMap.containsKey(expenseType.getParentId())) {
                roots.add(node);
                continue;
            }
            nodeMap.get(expenseType.getParentId()).getChildren().add(node);
        }
        return roots;
    }

    private ProcessExpenseTypeTreeVO toExpenseTypeTree(ProcessExpenseType expenseType) {
        ProcessExpenseTypeTreeVO treeNode = new ProcessExpenseTypeTreeVO();
        treeNode.setId(expenseType.getId());
        treeNode.setParentId(expenseType.getParentId());
        treeNode.setExpenseCode(expenseType.getExpenseCode());
        treeNode.setExpenseName(expenseType.getExpenseName());
        treeNode.setStatus(expenseType.getStatus());
        return treeNode;
    }

    private ProcessExpenseTypeDetailVO buildExpenseTypeDetail(ProcessExpenseType expenseType) {
        ProcessExpenseTypeDetailVO detail = new ProcessExpenseTypeDetailVO();
        detail.setId(expenseType.getId());
        detail.setParentId(expenseType.getParentId());
        detail.setExpenseCode(expenseType.getExpenseCode());
        detail.setExpenseName(expenseType.getExpenseName());
        detail.setExpenseDescription(expenseType.getExpenseDescription());
        detail.setCodeLevel(expenseType.getCodeLevel());
        detail.setCodePrefix(expenseType.getCodePrefix());
        detail.setScopeDeptIds(deserializeStringList(expenseType.getScopeDeptIds()));
        detail.setScopeUserIds(deserializeStringList(expenseType.getScopeUserIds()));
        detail.setInvoiceFreeMode(expenseType.getInvoiceFreeMode());
        detail.setTaxDeductionMode(expenseType.getTaxDeductionMode());
        detail.setTaxSeparationMode(expenseType.getTaxSeparationMode());
        detail.setStatus(expenseType.getStatus());
        return detail;
    }

    private void validateExpenseType(ProcessExpenseTypeSaveDTO dto, ProcessExpenseType existing) {
        String expenseCode = trimToEmpty(dto.getExpenseCode());
        if (!expenseCode.matches("\\d{6}(\\d{2})?")) {
            throw new IllegalArgumentException("费用类型编码仅支持 6 位或 8 位数字");
        }

        if (!EXPENSE_TYPE_INVOICE_MODES.contains(trimToEmpty(dto.getInvoiceFreeMode()))) {
            throw new IllegalArgumentException("不支持的免票配置");
        }
        if (!EXPENSE_TYPE_TAX_MODES.contains(trimToEmpty(dto.getTaxDeductionMode()))) {
            throw new IllegalArgumentException("不支持的税额抵扣与转出配置");
        }
        if (!EXPENSE_TYPE_SEPARATION_MODES.contains(trimToEmpty(dto.getTaxSeparationMode()))) {
            throw new IllegalArgumentException("不支持的价税分离配置");
        }

        ProcessExpenseType duplicated = findExpenseTypeByCode(expenseCode);
        if (duplicated != null && (existing == null || !Objects.equals(duplicated.getId(), existing.getId()))) {
            throw new IllegalArgumentException("费用类型编码已存在");
        }

        ProcessExpenseType parentExpenseType = null;
        if (expenseCode.length() == 8) {
            parentExpenseType = findExpenseTypeByCode(expenseCode.substring(0, 6));
            if (parentExpenseType == null) {
                throw new IllegalArgumentException("8 位费用类型编码必须先存在对应的 6 位上级编码");
            }
        }

        if (existing != null && !Objects.equals(existing.getExpenseCode(), expenseCode)) {
            if (hasExpenseTypeChildren(existing.getId())) {
                throw new IllegalStateException("当前费用类型存在下级节点，不能直接修改编码");
            }
            if (isExpenseTypeReferenced(existing)) {
                throw new IllegalStateException("当前费用类型已被模板引用，不能直接修改编码");
            }
        }

        Integer targetStatus = normalizeStatus(dto.getStatus());
        if (targetStatus == 1 && parentExpenseType != null && !Objects.equals(parentExpenseType.getStatus(), 1)) {
            throw new IllegalStateException("上级费用类型未启用，当前二级费用类型不能启用");
        }

        validateSelectableIds(normalizeIdList(dto.getScopeDeptIds()), loadValidDepartmentIdSet(), "部门");
        validateSelectableIds(normalizeIdList(dto.getScopeUserIds()), loadValidUserIdSet(), "人员");
    }

    private void applyExpenseTypeBase(ProcessExpenseType expenseType, ProcessExpenseTypeSaveDTO dto) {
        String expenseCode = trimToEmpty(dto.getExpenseCode());
        ProcessExpenseType parentExpenseType = expenseCode.length() == 8 ? findExpenseTypeByCode(expenseCode.substring(0, 6)) : null;

        expenseType.setParentId(parentExpenseType == null ? null : parentExpenseType.getId());
        expenseType.setExpenseCode(expenseCode);
        expenseType.setExpenseName(trimToEmpty(dto.getExpenseName()));
        expenseType.setExpenseDescription(trimToNull(dto.getExpenseDescription()));
        expenseType.setCodeLevel(expenseCode.length() == 6 ? 1 : 2);
        expenseType.setCodePrefix(expenseCode.substring(0, 4));
        expenseType.setScopeDeptIds(serializeStringList(dto.getScopeDeptIds()));
        expenseType.setScopeUserIds(serializeStringList(dto.getScopeUserIds()));
        expenseType.setInvoiceFreeMode(trimToEmpty(dto.getInvoiceFreeMode()));
        expenseType.setTaxDeductionMode(trimToEmpty(dto.getTaxDeductionMode()));
        expenseType.setTaxSeparationMode(trimToEmpty(dto.getTaxSeparationMode()));
        expenseType.setStatus(normalizeStatus(dto.getStatus()));
    }

    private void validateExpenseTypeStatus(ProcessExpenseType expenseType, Integer status) {
        if (status != 1 || expenseType.getParentId() == null) {
            return;
        }
        ProcessExpenseType parentExpenseType = processExpenseTypeMapper.selectById(expenseType.getParentId());
        if (parentExpenseType != null && !Objects.equals(parentExpenseType.getStatus(), 1)) {
            throw new IllegalStateException("上级费用类型未启用，当前节点不能启用");
        }
    }

    private void disableExpenseTypeChildren(Long parentId) {
        List<ProcessExpenseType> children = processExpenseTypeMapper.selectList(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .eq(ProcessExpenseType::getParentId, parentId)
        );
        for (ProcessExpenseType child : children) {
            if (!Objects.equals(child.getStatus(), 0)) {
                child.setStatus(0);
                processExpenseTypeMapper.updateById(child);
            }
            disableExpenseTypeChildren(child.getId());
        }
    }

    private ProcessExpenseType requireExpenseType(Long id) {
        ProcessExpenseType expenseType = processExpenseTypeMapper.selectById(id);
        if (expenseType == null) {
            throw new IllegalStateException("未找到对应的费用类型");
        }
        return expenseType;
    }

    private boolean hasExpenseTypeChildren(Long id) {
        Long count = processExpenseTypeMapper.selectCount(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .eq(ProcessExpenseType::getParentId, id)
        );
        return count != null && count > 0;
    }

    private boolean isExpenseTypeReferenced(ProcessExpenseType expenseType) {
        Long count = scopeMapper.selectCount(
                Wrappers.<ProcessTemplateScope>lambdaQuery()
                        .eq(ProcessTemplateScope::getOptionType, "EXPENSE_TYPE")
                        .eq(ProcessTemplateScope::getOptionCode, expenseType.getExpenseCode())
        );
        return count != null && count > 0;
    }

    private ProcessExpenseType findExpenseTypeByCode(String expenseCode) {
        return processExpenseTypeMapper.selectOne(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .eq(ProcessExpenseType::getExpenseCode, expenseCode)
                        .last("limit 1")
        );
    }

    private ProcessExpenseTypeConfigOptionVO configOption(String value, String label, String description) {
        ProcessExpenseTypeConfigOptionVO option = new ProcessExpenseTypeConfigOptionVO();
        option.setValue(value);
        option.setLabel(label);
        option.setDescription(description);
        return option;
    }

    private List<ProcessFormOptionVO> loadDepartmentOptions() {
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .select(SystemDepartment::getId, SystemDepartment::getDeptName)
                        .eq(SystemDepartment::getStatus, 1)
                        .orderByAsc(SystemDepartment::getSortOrder, SystemDepartment::getId)
        ).stream().map(department -> option(department.getDeptName(), String.valueOf(department.getId()))).toList();
    }

    private List<ProcessFormOptionVO> loadUserOptions() {
        return userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getStatus, 1)
                        .orderByAsc(User::getId)
        ).stream().map(user -> {
            String label = trimToNull(user.getName()) != null ? user.getName() : normalize(user.getUsername(), "未命名人员");
            if (trimToNull(user.getUsername()) != null && !Objects.equals(label, user.getUsername())) {
                label = label + " (" + user.getUsername() + ")";
            }
            return option(label, String.valueOf(user.getId()));
        }).toList();
    }

    private Set<String> loadValidDepartmentIdSet() {
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .eq(SystemDepartment::getStatus, 1)
                        .select(SystemDepartment::getId)
        ).stream().map(item -> String.valueOf(item.getId())).collect(Collectors.toSet());
    }

    private Set<String> loadValidUserIdSet() {
        return userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getStatus, 1)
                        .select(User::getId)
        ).stream().map(item -> String.valueOf(item.getId())).collect(Collectors.toSet());
    }

    private void validateSelectableIds(List<String> selectedIds, Set<String> validIds, String fieldName) {
        for (String selectedId : selectedIds) {
            if (!validIds.contains(selectedId)) {
                throw new IllegalArgumentException(fieldName + "范围中存在无效数据: " + selectedId);
            }
        }
    }

    private List<String> normalizeIdList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String normalizedValue = trimToNull(value);
            if (normalizedValue != null) {
                result.add(normalizedValue);
            }
        }
        return new ArrayList<>(result);
    }

    private String serializeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(normalizeIdList(values));
        } catch (Exception ex) {
            throw new IllegalStateException("范围数据序列化失败", ex);
        }
    }

    private List<String> deserializeStringList(String json) {
        if (trimToNull(json) == null) {
            return Collections.emptyList();
        }
        try {
            List<String> values = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            return normalizeIdList(values);
        } catch (Exception ex) {
            throw new IllegalStateException("范围数据反序列化失败", ex);
        }
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        return status == 1 ? 1 : 0;
    }

    private String normalizeComparable(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        String normalizedValue = trimToNull(String.valueOf(value));
        if (normalizedValue == null) {
            return null;
        }
        try {
            return new BigDecimal(normalizedValue);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalize(String value, String defaultValue) {
        String normalizedValue = trimToNull(value);
        return normalizedValue == null ? defaultValue : normalizedValue;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME_FORMATTER.format(dateTime);
    }

    private ProcessCenterNavItemVO navItem(String key, String label, String tip) {
        ProcessCenterNavItemVO item = new ProcessCenterNavItemVO();
        item.setKey(key);
        item.setLabel(label);
        item.setTip(tip);
        return item;
    }

    private ProcessTemplateTypeVO templateType(String code, String name, String subtitle, String description, String accent) {
        ProcessTemplateTypeVO type = new ProcessTemplateTypeVO();
        type.setCode(code);
        type.setName(name);
        type.setSubtitle(subtitle);
        type.setDescription(description);
        type.setAccent(accent);
        return type;
    }

    private ProcessFormOptionVO option(String label, String value) {
        ProcessFormOptionVO option = new ProcessFormOptionVO();
        option.setLabel(label);
        option.setValue(value);
        return option;
    }

    private record RuleFieldDefinition(
            String key,
            String label,
            String valueType,
            List<String> operatorKeys
    ) {
    }
}
