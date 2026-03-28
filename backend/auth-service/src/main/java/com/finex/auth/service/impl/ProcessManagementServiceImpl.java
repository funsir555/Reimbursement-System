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
import com.finex.auth.dto.ProcessFlowDetailVO;
import com.finex.auth.dto.ProcessFlowMetaVO;
import com.finex.auth.dto.ProcessFlowResolveApproversDTO;
import com.finex.auth.dto.ProcessFlowResolveApproversVO;
import com.finex.auth.dto.ProcessFlowSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneVO;
import com.finex.auth.dto.ProcessFlowSummaryVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.dto.ProcessTemplateCardVO;
import com.finex.auth.dto.ProcessTemplateCategoryVO;
import com.finex.auth.dto.ProcessTemplateDetailVO;
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
import com.finex.auth.service.ProcessFlowDesignService;
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
    private static final String DEFAULT_NUMBERING_RULE_PREVIEW = "FX+\u5e74+\u6708+\u65e5+4\u4f4d\u6570\u5b57\uff08\u5982\uff1aFX202503251234\uff09";
    private static final String DEFAULT_TEMPLATE_COLOR = "blue";
    private static final String SCOPE_TYPE_DEPARTMENT = "SCOPE_DEPARTMENT";
    private static final String SCOPE_TYPE_EXPENSE_TYPE = "SCOPE_EXPENSE_TYPE";
    private static final String SCOPE_TYPE_AMOUNT_MIN = "SCOPE_AMOUNT_MIN";
    private static final String SCOPE_TYPE_AMOUNT_MAX = "SCOPE_AMOUNT_MAX";
    private static final String SCOPE_TYPE_TAG_ARCHIVE = "TAG_ARCHIVE";
    private static final String SCOPE_TYPE_INSTALLMENT_ARCHIVE = "INSTALLMENT_ARCHIVE";

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
            Map.entry("EQ", "\u7b49\u4e8e"),
            Map.entry("NE", "\u4e0d\u7b49\u4e8e"),
            Map.entry("IN", "\u5c5e\u4e8e"),
            Map.entry("NOT_IN", "\u4e0d\u5c5e\u4e8e"),
            Map.entry("GT", "\u5927\u4e8e"),
            Map.entry("GE", "\u5927\u4e8e\u7b49\u4e8e"),
            Map.entry("LT", "\u5c0f\u4e8e"),
            Map.entry("LE", "\u5c0f\u4e8e\u7b49\u4e8e"),
            Map.entry("BETWEEN", "\u4ecb\u4e8e"),
            Map.entry("CONTAINS", "\u5305\u542b")
    );

    private static final List<RuleFieldDefinition> RULE_FIELD_DEFINITIONS = List.of(
            new RuleFieldDefinition("submitterDeptId", "\u63d0\u5355\u4eba\u90e8\u95e8", FIELD_VALUE_TYPE_DEPARTMENT, List.of("EQ", "NE", "IN", "NOT_IN")),
            new RuleFieldDefinition("submitterPosition", "\u63d0\u5355\u4eba\u5c97\u4f4d", FIELD_VALUE_TYPE_TEXT, List.of("EQ", "NE", "IN", "NOT_IN", "CONTAINS")),
            new RuleFieldDefinition("laborRelationBelong", "\u52b3\u52a8\u5173\u7cfb\u5f52\u5c5e", FIELD_VALUE_TYPE_TEXT, List.of("EQ", "NE", "IN", "NOT_IN", "CONTAINS")),
            new RuleFieldDefinition("documentType", "\u5355\u636e\u7c7b\u578b", FIELD_VALUE_TYPE_TEXT, List.of("EQ", "NE", "IN", "NOT_IN")),
            new RuleFieldDefinition("amount", "\u91d1\u989d", FIELD_VALUE_TYPE_NUMBER, List.of("EQ", "NE", "GT", "GE", "LT", "LE", "BETWEEN"))
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
    private final ProcessFlowDesignService processFlowDesignService;
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
                templateType("report", "\u62a5\u9500\u5355", "\u8d39\u7528\u62a5\u9500", "\u9002\u7528\u4e8e\u5458\u5de5\u62a5\u9500\u3001\u5dee\u65c5\u62a5\u9500\u4e0e\u56e2\u961f\u8d39\u7528\u5f52\u96c6\u7b49\u573a\u666f\u3002", "blue"),
                templateType("application", "\u7533\u8bf7\u5355", "\u4e1a\u52a1\u7533\u8bf7", "\u9002\u7528\u4e8e\u9884\u7b97\u7533\u8bf7\u3001\u4ed8\u6b3e\u7533\u8bf7\u3001\u9879\u76ee\u7533\u8bf7\u7b49\u4e8b\u524d\u6d41\u7a0b\u3002", "cyan"),
                templateType("loan", "\u501f\u6b3e\u5355", "\u501f\u652f\u7ba1\u7406", "\u9002\u7528\u4e8e\u5907\u7528\u91d1\u501f\u652f\u3001\u9879\u76ee\u501f\u6b3e\u53ca\u540e\u7eed\u6838\u9500\u5f52\u8fd8\u573a\u666f\u3002", "orange")
        );
    }

    @Override
    public ProcessTemplateFormOptionsVO getFormOptions(String templateType) {
        ProcessTemplateFormOptionsVO options = new ProcessTemplateFormOptionsVO();
        options.setTemplateType(templateType);
        options.setTemplateTypeLabel(resolveTemplateTypeLabel(templateType));
        options.setCategoryOptions(loadTemplateCategoryOptions());
        options.setNumberingRulePreview(DEFAULT_NUMBERING_RULE_PREVIEW);
        options.setFormDesignOptions(loadFormDesignOptions(templateType));
        options.setApprovalFlows(processFlowDesignService.listPublishedFlowOptions());
        options.setPrintModes(List.of(
                option("\u9ed8\u8ba4\u6253\u5370\u6a21\u677f", "default-print"),
                option("\u6a2a\u7248\u6458\u8981\u6a21\u677f", "landscape-summary"),
                option("\u8d22\u52a1\u5f52\u6863\u6a21\u677f", "finance-archive")
        ));
        options.setPaymentModes(List.of(
                option("\u4e0d\u751f\u6210\u4ed8\u6b3e\u5355", "none"),
                option("\u751f\u6210\u5bf9\u79c1\u4ed8\u6b3e\u5355", "private-payment"),
                option("\u751f\u6210\u5bf9\u516c\u4ed8\u6b3e\u5355", "public-payment")
        ));
        options.setAllocationForms(List.of(
                option("\u9ed8\u8ba4\u5206\u644a\u8868", "allocation-default"),
                option("\u9879\u76ee\u5206\u644a\u8868", "allocation-project"),
                option("\u90e8\u95e8\u5206\u644a\u8868", "allocation-department")
        ));
        options.setExpenseTypes(loadEnabledExpenseTypeTree());
        options.setDepartmentOptions(loadDepartmentOptions());
        options.setAiAuditModes(List.of(
                option("\u5173\u95ed AI \u5ba1\u6838", "disabled"),
                option("\u6807\u51c6\u98ce\u9669\u8bc6\u522b", "standard"),
                option("\u4e25\u683c\u98ce\u9669\u8bc6\u522b", "strict")
        ));
        options.setTagOptions(loadEnabledArchiveOptions());
        options.setInstallmentOptions(loadEnabledArchiveOptions());
        return options;
    }

    @Override
    public ProcessTemplateDetailVO getTemplateDetail(Long id) {
        return buildTemplateDetail(requireTemplate(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessTemplateSaveResultVO saveTemplate(ProcessTemplateSaveDTO dto, String operatorName) {
        String categoryCode = normalize(dto.getCategory(), "employee-expense");
        String templateType = normalize(dto.getTemplateType(), "report");
        boolean enabled = dto.getEnabled() == null || dto.getEnabled();

        validateTemplateScope(dto);

        Map<String, String> departmentLabelMap = departmentLabelMap();
        Map<String, String> expenseTypeLabelMap = expenseTypeLabelMap();
        Map<String, String> archiveLabelMap = enabledArchiveLabelMap();
        Map<String, String> flowLabelMap = processFlowDesignService.publishedFlowLabelMap();
        String approvalFlowCode = resolveApprovalFlowCode(dto.getApprovalFlow(), flowLabelMap);

        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateCode(buildTemplateCode());
        template.setTemplateName(trimToEmpty(dto.getTemplateName()));
        template.setTemplateType(templateType);
        template.setTemplateTypeLabel(resolveTemplateTypeLabel(templateType));
        template.setCategoryCode(categoryCode);
        template.setTemplateDescription(resolveDescription(dto));
        template.setNumberingRule(DEFAULT_NUMBERING_RULE_CODE);
        template.setFormDesignCode(resolveFormDesignCode(dto.getFormDesign(), templateType));
        template.setIconColor(DEFAULT_TEMPLATE_COLOR);
        template.setEnabled(enabled ? 1 : 0);
        template.setPublishStatus(enabled ? "ENABLED" : "DRAFT");
        template.setPrintMode(normalize(dto.getPrintMode(), "default-print"));
        template.setApprovalFlow(approvalFlowCode);
        template.setFlowName(flowLabelMap.get(approvalFlowCode));
        template.setPaymentMode(normalize(dto.getPaymentMode(), "none"));
        template.setAllocationForm(normalize(dto.getAllocationForm(), "allocation-default"));
        template.setAiAuditMode(normalize(dto.getAiAuditMode(), "disabled"));
        template.setHighlights(String.join(HIGHLIGHT_SEPARATOR, buildHighlights(dto, archiveLabelMap)));
        template.setOwnerName(normalize(operatorName, "\u6d41\u7a0b\u7ba1\u7406\u5458"));
        template.setSortOrder(nextSortOrder(categoryCode));
        templateMapper.insert(template);

        replaceTemplateScopes(template.getId(), dto, departmentLabelMap, expenseTypeLabelMap, archiveLabelMap);
        return buildTemplateSaveResult(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessTemplateSaveResultVO updateTemplate(Long id, ProcessTemplateSaveDTO dto, String operatorName) {
        ProcessDocumentTemplate template = requireTemplate(id);
        String categoryCode = normalize(dto.getCategory(), template.getCategoryCode());
        String templateType = normalize(dto.getTemplateType(), template.getTemplateType());
        boolean enabled = dto.getEnabled() == null || dto.getEnabled();

        validateTemplateScope(dto);

        Map<String, String> departmentLabelMap = departmentLabelMap();
        Map<String, String> expenseTypeLabelMap = expenseTypeLabelMap();
        Map<String, String> archiveLabelMap = enabledArchiveLabelMap();
        Map<String, String> flowLabelMap = processFlowDesignService.publishedFlowLabelMap();
        String approvalFlowCode = resolveApprovalFlowCode(dto.getApprovalFlow(), flowLabelMap);

        template.setTemplateName(trimToEmpty(dto.getTemplateName()));
        template.setTemplateType(templateType);
        template.setTemplateTypeLabel(resolveTemplateTypeLabel(templateType));
        template.setCategoryCode(categoryCode);
        template.setTemplateDescription(resolveDescription(dto));
        template.setNumberingRule(DEFAULT_NUMBERING_RULE_CODE);
        template.setFormDesignCode(resolveFormDesignCode(dto.getFormDesign(), templateType));
        template.setIconColor(DEFAULT_TEMPLATE_COLOR);
        template.setEnabled(enabled ? 1 : 0);
        template.setPublishStatus(enabled ? "ENABLED" : "DRAFT");
        template.setPrintMode(normalize(dto.getPrintMode(), "default-print"));
        template.setApprovalFlow(approvalFlowCode);
        template.setFlowName(flowLabelMap.get(approvalFlowCode));
        template.setPaymentMode(normalize(dto.getPaymentMode(), "none"));
        template.setAllocationForm(normalize(dto.getAllocationForm(), "allocation-default"));
        template.setAiAuditMode(normalize(dto.getAiAuditMode(), "disabled"));
        template.setHighlights(String.join(HIGHLIGHT_SEPARATOR, buildHighlights(dto, archiveLabelMap)));
        template.setOwnerName(normalize(operatorName, template.getOwnerName()));
        templateMapper.updateById(template);

        replaceTemplateScopes(template.getId(), dto, departmentLabelMap, expenseTypeLabelMap, archiveLabelMap);
        return buildTemplateSaveResult(template);
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
                throw new IllegalStateException("\u5f53\u524d\u6863\u6848\u7ed3\u679c\u9879\u5df2\u88ab\u6a21\u677f\u5f15\u7528\uff0c\u4e0d\u80fd\u5220\u9664\u6863\u6848");
            }
        }

        Long archiveReferencedCount = scopeMapper.selectCount(
                Wrappers.<ProcessTemplateScope>lambdaQuery()
                        .in(ProcessTemplateScope::getOptionType, List.of(SCOPE_TYPE_TAG_ARCHIVE, SCOPE_TYPE_INSTALLMENT_ARCHIVE))
                        .eq(ProcessTemplateScope::getOptionCode, archive.getArchiveCode())
        );
        if (archiveReferencedCount != null && archiveReferencedCount > 0) {
            throw new IllegalStateException("\u5f53\u524d\u6863\u6848\u5df2\u88ab\u6a21\u677f\u4f5c\u4e3a\u6807\u7b7e\u6216\u5206\u671f\u4ed8\u6b3e\u6765\u6e90\u5f15\u7528\uff0c\u4e0d\u80fd\u5220\u9664");
        }

        deleteArchiveChildren(id);
        customArchiveDesignMapper.deleteById(archive.getId());
        return Boolean.TRUE;
    }

    @Override
    public ProcessCustomArchiveMetaVO getCustomArchiveMeta() {
        ProcessCustomArchiveMetaVO meta = new ProcessCustomArchiveMetaVO();
        meta.setArchiveTypeOptions(List.of(
                option("\u63d0\u4f9b\u9009\u62e9", ARCHIVE_TYPE_SELECT),
                option("\u81ea\u52a8\u5212\u5206", ARCHIVE_TYPE_AUTO_RULE)
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
                configOption(EXPENSE_TYPE_INVOICE_FREE, "\u514d\u7968", "\u9ed8\u8ba4\u65e0\u9700\u4e0a\u4f20\u53d1\u7968\uff0c\u4e14\u8d39\u7528\u81ea\u52a8\u6807\u8bb0\u4e3a\u514d\u7968"),
                configOption(EXPENSE_TYPE_INVOICE_REQUIRED, "\u4e0d\u514d\u7968", "\u6839\u636e\u8d39\u7528\u8868\u5355\u4e2d\u53d1\u7968\u7ec4\u4ef6\u7684\u5fc5\u586b\u6027\u8fdb\u884c\u5224\u65ad")
        ));
        meta.setTaxDeductionOptions(List.of(
                configOption(EXPENSE_TYPE_TAX_DEFAULT, "\u9075\u5faa\u9ed8\u8ba4\u62b5\u6263\u548c\u8f6c\u51fa\u903b\u8f91", "\u6cbf\u7528\u7cfb\u7edf\u9ed8\u8ba4\u7684\u62b5\u6263\u4e0e\u8f6c\u51fa\u5904\u7406\u903b\u8f91"),
                configOption(EXPENSE_TYPE_TAX_SPECIAL_NO_DEDUCT_NEED_OUT, "\u4e0a\u4f20\u4e13\u7968\u65f6\u9ed8\u8ba4\u4e0d\u62b5\u6263\u4e14\u9700\u8981\u8f6c\u51fa\uff0c\u5176\u4ed6\u53d1\u7968\u9075\u5faa\u9ed8\u8ba4\u903b\u8f91", "\u9002\u7528\u4e8e\u798f\u5229\u8d39\u5f00\u4e13\u7968\u7b49\u573a\u666f"),
                configOption(EXPENSE_TYPE_TAX_SPECIAL_NO_DEDUCT_NEED_OUT_OTHERS_NONE, "\u4e0a\u4f20\u4e13\u7968\u65f6\u9ed8\u8ba4\u4e0d\u62b5\u6263\u4e14\u9700\u8981\u8f6c\u51fa\uff0c\u5176\u4ed6\u53d1\u7968\u4e0d\u62b5\u6263\u4e0d\u8f6c\u51fa", "\u9002\u7528\u4e8e\u62a5\u9500\u798f\u5229\u8d39\u7b49\u573a\u666f"),
                configOption(EXPENSE_TYPE_TAX_ALL_NO_DEDUCT_NO_OUT, "\u65e0\u8bba\u4e0a\u4f20\u4efb\u4f55\u7968\u79cd\uff0c\u9ed8\u8ba4\u4e0d\u62b5\u6263\u4e14\u65e0\u9700\u8f6c\u51fa", "\u9002\u7528\u4e8e\u5ba2\u6237\u673a\u7968\u7b49\u573a\u666f"),
                configOption(EXPENSE_TYPE_TAX_HAS_DEDUCT_NO_DEDUCT_NEED_OUT, "\u4e0a\u4f20\u6709\u62b5\u6263\u7a0e\u989d\u7684\u53d1\u7968\u65f6\uff0c\u9ed8\u8ba4\u4e0d\u62b5\u6263\u4e14\u9700\u8981\u8f6c\u51fa", "\u9002\u7528\u4e8e\u5ba2\u6237\u98de\u673a\u706b\u8f66\u798f\u5229\u8d39\u7b49\u573a\u666f")
        ));
        meta.setTaxSeparationOptions(List.of(
                configOption(EXPENSE_TYPE_TAX_SEPARATE, "\u4ef7\u7a0e\u5206\u79bb", "\u8d39\u7528\u91d1\u989d\u4e0e\u7a0e\u989d\u5206\u5f00\u5904\u7406"),
                configOption(EXPENSE_TYPE_TAX_NOT_SEPARATE, "\u4ef7\u7a0e\u4e0d\u5206\u79bb", "\u8d39\u7528\u91d1\u989d\u4e0e\u7a0e\u989d\u5408\u5e76\u5904\u7406")
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
            throw new IllegalStateException("\u5f53\u524d\u8d39\u7528\u7c7b\u578b\u4e0b\u5b58\u5728\u5b50\u7ea7\u8282\u70b9\uff0c\u4e0d\u80fd\u5220\u9664");
        }
        if (isExpenseTypeReferenced(expenseType)) {
            throw new IllegalStateException("\u5f53\u524d\u8d39\u7528\u7c7b\u578b\u5df2\u88ab\u6a21\u677f\u5f15\u7528\uff0c\u4e0d\u80fd\u5220\u9664");
        }
        processExpenseTypeMapper.deleteById(id);
        return Boolean.TRUE;
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

    private List<ProcessCenterNavItemVO> buildNavItems() {
        return List.of(
                navItem("document-flow", "\u5355\u636e\u4e0e\u6d41\u7a0b", "\u7ef4\u62a4\u5355\u636e\u6a21\u677f\u3001\u5ba1\u6279\u6d41\u7a0b\u548c\u76f8\u5173\u914d\u7f6e\u80fd\u529b"),
                navItem("custom-archive", "\u81ea\u5b9a\u4e49\u6863\u6848", "\u7ef4\u62a4\u6807\u7b7e\u3001\u5206\u671f\u4ed8\u6b3e\u7b49\u4e1a\u52a1\u914d\u7f6e\u6863\u6848"),
                navItem("expense-type", "\u8d39\u7528\u7c7b\u578b", "\u7ef4\u62a4\u8d39\u7528\u7c7b\u578b\u6811\u548c\u53d1\u7968\u7a0e\u52a1\u914d\u7f6e")
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
            card.setDescription(normalize(category.getCategoryDescription(), "\u7ef4\u62a4\u8be5\u5206\u7c7b\u4e0b\u7684\u6d41\u7a0b\u6a21\u677f"));
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
            card.setDescription("\u672a\u5f52\u7c7b\u6a21\u677f");
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
            card.setTemplateTypeCode(template.getTemplateType());
            card.setTemplateType(normalize(template.getTemplateTypeLabel(), resolveTemplateTypeLabel(template.getTemplateType())));
            card.setBusinessDomain(categoryName);
            card.setDescription(normalize(template.getTemplateDescription(), "\u7ef4\u62a4\u8be5\u6a21\u677f\u7684\u914d\u7f6e\u8bf4\u660e"));
            card.setHighlights(splitHighlights(template.getHighlights()));
            card.setFlowName(normalize(template.getFlowName(), "\u672a\u8bbe\u7f6e\u5ba1\u6279\u6d41\u7a0b"));
            card.setUpdatedAt(formatDateTime(template.getUpdatedAt()));
            card.setOwner(normalize(template.getOwnerName(), "\u6d41\u7a0b\u7ba1\u7406\u5458"));
            card.setColor(resolveColor(template.getIconColor()));
            return card;
        }).toList();
    }

    private ProcessTemplateDetailVO buildTemplateDetail(ProcessDocumentTemplate template) {
        Map<String, List<ProcessTemplateScope>> scopeMap = loadTemplateScopeMap(template.getId());

        ProcessTemplateDetailVO detail = new ProcessTemplateDetailVO();
        detail.setId(template.getId());
        detail.setTemplateCode(template.getTemplateCode());
        detail.setTemplateType(template.getTemplateType());
        detail.setTemplateTypeLabel(normalize(template.getTemplateTypeLabel(), resolveTemplateTypeLabel(template.getTemplateType())));
        detail.setTemplateName(template.getTemplateName());
        detail.setTemplateDescription(template.getTemplateDescription());
        detail.setCategory(template.getCategoryCode());
        detail.setEnabled(template.getEnabled() == null || template.getEnabled() == 1);
        detail.setFormDesign(template.getFormDesignCode());
        detail.setPrintMode(template.getPrintMode());
        detail.setApprovalFlow(template.getApprovalFlow());
        detail.setPaymentMode(template.getPaymentMode());
        detail.setAllocationForm(template.getAllocationForm());
        detail.setAiAuditMode(template.getAiAuditMode());
        detail.setScopeDeptIds(extractScopeCodes(scopeMap.get(SCOPE_TYPE_DEPARTMENT)));
        detail.setScopeExpenseTypeCodes(extractScopeCodes(scopeMap.get(SCOPE_TYPE_EXPENSE_TYPE)));
        detail.setAmountMin(parseScopeAmount(scopeMap.get(SCOPE_TYPE_AMOUNT_MIN)));
        detail.setAmountMax(parseScopeAmount(scopeMap.get(SCOPE_TYPE_AMOUNT_MAX)));
        detail.setTagOption(resolveArchiveScopeCode(scopeMap.get(SCOPE_TYPE_TAG_ARCHIVE), scopeMap.get("TAG_OPTION")));
        detail.setInstallmentOption(resolveArchiveScopeCode(scopeMap.get(SCOPE_TYPE_INSTALLMENT_ARCHIVE), scopeMap.get("INSTALLMENT_OPTION")));
        return detail;
    }

    private ProcessTemplateSaveResultVO buildTemplateSaveResult(ProcessDocumentTemplate template) {
        ProcessTemplateSaveResultVO result = new ProcessTemplateSaveResultVO();
        result.setId(template.getId());
        result.setTemplateCode(template.getTemplateCode());
        result.setTemplateName(template.getTemplateName());
        result.setStatus(template.getPublishStatus());
        return result;
    }

    private Map<String, List<ProcessTemplateScope>> loadTemplateScopeMap(Long templateId) {
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

    private List<String> extractScopeCodes(List<ProcessTemplateScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return Collections.emptyList();
        }
        return scopes.stream()
                .map(ProcessTemplateScope::getOptionCode)
                .filter(Objects::nonNull)
                .toList();
    }

    private BigDecimal parseScopeAmount(List<ProcessTemplateScope> scopes) {
        String value = firstScopeCode(scopes);
        if (value == null) {
            return null;
        }
        return new BigDecimal(value);
    }

    private String firstScopeCode(List<ProcessTemplateScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return null;
        }
        return trimToNull(scopes.get(0).getOptionCode());
    }

    private String resolveArchiveScopeCode(List<ProcessTemplateScope> archiveScopes, List<ProcessTemplateScope> legacyScopes) {
        String archiveCode = firstScopeCode(archiveScopes);
        if (archiveCode != null) {
            return archiveCode;
        }

        String legacyItemCode = firstScopeCode(legacyScopes);
        if (legacyItemCode == null) {
            return "";
        }
        return normalize(findArchiveCodeByLegacyItemCode(legacyItemCode), "");
    }

    private String findArchiveCodeByLegacyItemCode(String itemCode) {
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

    private List<String> splitHighlights(String highlights) {
        if (trimToNull(highlights) == null) {
            return List.of("\u6682\u65e0\u4eae\u70b9");
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
                option("\u5458\u5de5\u62a5\u9500", "employee-expense"),
                option("\u5bf9\u516c\u4ed8\u6b3e", "enterprise-payment"),
                option("\u4e1a\u52a1\u7533\u8bf7", "business-application")
        );
    }

    private List<ProcessFormOptionVO> loadFormDesignOptions(String templateType) {
        return switch (normalize(templateType, "report")) {
            case "application" -> List.of(
                    option("\u6807\u51c6\u7533\u8bf7\u8868\u5355", "application-standard-form"),
                    option("\u9879\u76ee\u7533\u8bf7\u8868\u5355", "application-project-form"),
                    option("\u5bf9\u516c\u7533\u8bf7\u8868\u5355", "application-public-form")
            );
            case "loan" -> List.of(
                    option("\u6807\u51c6\u501f\u6b3e\u8868\u5355", "loan-standard-form"),
                    option("\u5dee\u65c5\u501f\u6b3e\u8868\u5355", "loan-travel-form"),
                    option("\u5907\u7528\u91d1\u501f\u6b3e\u8868\u5355", "loan-petty-cash-form")
            );
            default -> List.of(
                    option("\u6807\u51c6\u62a5\u9500\u8868\u5355", "expense-standard-form"),
                    option("\u5dee\u65c5\u62a5\u9500\u8868\u5355", "expense-travel-form"),
                    option("\u5bf9\u516c\u62a5\u9500\u8868\u5355", "expense-public-form")
            );
        };
    }

    private String resolveFormDesignCode(String formDesign, String templateType) {
        List<ProcessFormOptionVO> options = loadFormDesignOptions(templateType);
        String normalized = trimToNull(formDesign);
        if (normalized == null) {
            return options.get(0).getValue();
        }
        boolean matched = options.stream().anyMatch(option -> Objects.equals(option.getValue(), normalized));
        if (!matched) {
            throw new IllegalArgumentException("\u6240\u9009\u8868\u5355\u8bbe\u8ba1\u4e0d\u5b58\u5728\u6216\u4e0d\u5c5e\u4e8e\u5f53\u524d\u6a21\u677f\u7c7b\u578b");
        }
        return normalized;
    }

    private void validateTemplateScope(ProcessTemplateSaveDTO dto) {
        validateSelectableIds(normalizeIdList(dto.getScopeDeptIds()), loadValidDepartmentIdSet(), "\u90e8\u95e8");
        validateSelectableIds(normalizeIdList(dto.getScopeExpenseTypeCodes()), loadValidExpenseTypeCodeSet(), "\u8d39\u7528\u7c7b\u578b");

        BigDecimal amountMin = dto.getAmountMin();
        BigDecimal amountMax = dto.getAmountMax();
        if (amountMin != null && amountMin.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("\u6700\u5c0f\u91d1\u989d\u4e0d\u80fd\u5c0f\u4e8e 0");
        }
        if (amountMax != null && amountMax.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("\u6700\u5927\u91d1\u989d\u4e0d\u80fd\u5c0f\u4e8e 0");
        }
        if (amountMin != null && amountMax != null && amountMin.compareTo(amountMax) > 0) {
            throw new IllegalArgumentException("\u9650\u5b9a\u91d1\u989d\u533a\u95f4\u4e0d\u5408\u6cd5\uff0c\u6700\u5c0f\u91d1\u989d\u4e0d\u80fd\u5927\u4e8e\u6700\u5927\u91d1\u989d");
        }
    }

    private List<String> buildHighlights(
            ProcessTemplateSaveDTO dto,
            Map<String, String> archiveLabelMap
    ) {
        LinkedHashSet<String> highlights = new LinkedHashSet<>();
        highlights.add("\u79fb\u52a8\u7aef\u63d0\u5355");
        if (!"none".equalsIgnoreCase(normalize(dto.getPaymentMode(), "none"))) {
            highlights.add("\u4ed8\u6b3e\u5355\u8054\u52a8");
        }
        if (!"disabled".equalsIgnoreCase(normalize(dto.getAiAuditMode(), "disabled"))) {
            highlights.add("AI \u5ba1\u6838");
        }

        String tagLabel = archiveLabelMap.get(trimToEmpty(dto.getTagOption()));
        if (tagLabel != null) {
            highlights.add(tagLabel);
        }
        String installmentLabel = archiveLabelMap.get(trimToEmpty(dto.getInstallmentOption()));
        if (installmentLabel != null) {
            highlights.add(installmentLabel);
        }

        while (highlights.size() < 3) {
            highlights.add("\u6682\u65e0\u4eae\u70b9");
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

    private void replaceTemplateScopes(
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
        saveScopeItems(templateId, SCOPE_TYPE_DEPARTMENT, dto.getScopeDeptIds(), departmentLabelMap);
        saveScopeItems(templateId, SCOPE_TYPE_EXPENSE_TYPE, dto.getScopeExpenseTypeCodes(), expenseTypeLabelMap);
        saveAmountScopeItems(templateId, dto.getAmountMin(), dto.getAmountMax());
        saveSingleScopeItem(templateId, SCOPE_TYPE_TAG_ARCHIVE, dto.getTagOption(), archiveLabelMap);
        saveSingleScopeItem(templateId, SCOPE_TYPE_INSTALLMENT_ARCHIVE, dto.getInstallmentOption(), archiveLabelMap);
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
        return resolveTemplateTypeLabel(dto.getTemplateType()) + "\u6a21\u677f";
    }

    private String resolveTemplateTypeLabel(String templateType) {
        return switch (normalize(templateType, "report")) {
            case "application" -> "\u7533\u8bf7\u5355";
            case "loan" -> "\u501f\u6b3e\u5355";
            default -> "\u62a5\u9500\u5355";
        };
    }

    private String resolveApprovalFlowCode(String approvalFlow, Map<String, String> flowLabelMap) {
        String flowCode = trimToNull(approvalFlow);
        if (flowCode == null) {
            throw new IllegalArgumentException("\u8bf7\u9009\u62e9\u5ba1\u6279\u6d41\u7a0b");
        }
        if (!flowLabelMap.containsKey(flowCode)) {
            throw new IllegalArgumentException("\u5ba1\u6279\u6d41\u7a0b\u4e0d\u5b58\u5728\u6216\u5c1a\u672a\u53d1\u5e03");
        }
        return flowCode;
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

    private Map<String, String> departmentLabelMap() {
        return loadDepartmentOptions().stream().collect(Collectors.toMap(
                ProcessFormOptionVO::getValue,
                ProcessFormOptionVO::getLabel,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private void saveAmountScopeItems(Long templateId, BigDecimal amountMin, BigDecimal amountMax) {
        if (amountMin != null) {
            saveSingleScopeValue(templateId, SCOPE_TYPE_AMOUNT_MIN, amountMin.stripTrailingZeros().toPlainString(), "\u6700\u5c0f\u91d1\u989d", 1);
        }
        if (amountMax != null) {
            saveSingleScopeValue(templateId, SCOPE_TYPE_AMOUNT_MAX, amountMax.stripTrailingZeros().toPlainString(), "\u6700\u5927\u91d1\u989d", 2);
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

    private Map<String, String> scopeLabelMap() {
        Map<String, String> labelMap = new LinkedHashMap<>();
        labelMap.put("department", "\u9650\u5b9a\u90e8\u95e8\u4f7f\u7528");
        labelMap.put("position", "\u9650\u5b9a\u5c97\u4f4d\u4f7f\u7528");
        labelMap.put("expense-type", "\u9650\u5b9a\u8d39\u7528\u7c7b\u578b\u4f7f\u7528");
        labelMap.put("amount-range", "\u9650\u5b9a\u91d1\u989d");
        return labelMap;
    }

    private List<ProcessFormOptionVO> loadEnabledArchiveOptions() {
        return customArchiveDesignMapper.selectList(
                Wrappers.<ProcessCustomArchiveDesign>lambdaQuery()
                        .eq(ProcessCustomArchiveDesign::getStatus, 1)
                        .orderByDesc(ProcessCustomArchiveDesign::getUpdatedAt, ProcessCustomArchiveDesign::getId)
        ).stream().map(archive -> option(archive.getArchiveName(), archive.getArchiveCode())).toList();
    }

    private Map<String, String> enabledArchiveLabelMap() {
        return loadEnabledArchiveOptions().stream().collect(Collectors.toMap(
                ProcessFormOptionVO::getValue,
                ProcessFormOptionVO::getLabel,
                (left, right) -> left,
                LinkedHashMap::new
        ));
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

    private ProcessDocumentTemplate requireTemplate(Long id) {
        ProcessDocumentTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new IllegalStateException("模板不存在");
        }
        return template;
    }

    private ProcessCustomArchiveDesign requireCustomArchive(Long id) {
        ProcessCustomArchiveDesign archive = customArchiveDesignMapper.selectById(id);
        if (archive == null) {
            throw new IllegalStateException("\u81ea\u5b9a\u4e49\u6863\u6848\u4e0d\u5b58\u5728");
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
            throw new IllegalStateException("\u81ea\u5b9a\u4e49\u6863\u6848\u4e0d\u5b58\u5728");
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
            throw new IllegalArgumentException("闂傚倸鍊烽懗鍫曞储瑜旈妴鍐╂償閵忋埄娲稿┑鐘诧工閻楀﹪宕戦埡鍛厽闁逛即娼ф晶浼存煃缂佹ɑ绀€妞ゎ叀娉曢幑鍕瑹椤栨艾澹勯梻渚€鈧偛鑻晶顕€鏌涙繝鍐╁€愰柕鍡楁噺缁虹晫绮欓崸妤€鏁归梻渚€娼ф蹇曠礊閸℃鐒介柛顐ｆ礃閳锋垿鏌熼懖鈺佷粶濠碘€冲悑閵囧嫰骞嬪┑鍡╀紑闂佸憡甯楃敮妤呭箚閺冨牆惟闁靛／鍐╂啟濠碉紕鍋戦崐鏍偋濡ゅ啰鐭欓柟杈鹃檮閸?SELECT 闂?AUTO_RULE");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("\u8bf7\u81f3\u5c11\u6dfb\u52a0\u4e00\u4e2a\u7ed3\u679c\u9879");
        }

        for (ProcessCustomArchiveItemDTO item : dto.getItems()) {
            if (trimToNull(item.getItemName()) == null) {
                throw new IllegalArgumentException("\u7ed3\u679c\u9879\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
            }
            if (ARCHIVE_TYPE_AUTO_RULE.equals(dto.getArchiveType())) {
                validateRules(item.getRules());
            }
        }
    }

    private void validateRules(List<ProcessCustomArchiveRuleDTO> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("\u81ea\u52a8\u5212\u5206\u7c7b\u578b\u5fc5\u987b\u914d\u7f6e\u89c4\u5219");
        }
        for (ProcessCustomArchiveRuleDTO rule : rules) {
            if (rule.getGroupNo() == null || rule.getGroupNo() < 1) {
                throw new IllegalArgumentException("\u89c4\u5219\u7ec4\u5e8f\u53f7\u5fc5\u987b\u5927\u4e8e 0");
            }
            RuleFieldDefinition definition = RULE_FIELD_MAP.get(rule.getFieldKey());
            if (definition == null) {
                throw new IllegalArgumentException("\u4e0d\u652f\u6301\u7684\u89c4\u5219\u5b57\u6bb5: " + rule.getFieldKey());
            }
            if (!definition.operatorKeys().contains(rule.getOperator())) {
                throw new IllegalArgumentException("\u5b57\u6bb5 " + definition.label() + " \u4e0d\u652f\u6301\u64cd\u4f5c\u7b26 " + rule.getOperator());
            }
            if (rule.getCompareValue() == null) {
                throw new IllegalArgumentException("\u89c4\u5219\u6bd4\u8f83\u503c\u4e0d\u80fd\u4e3a\u7a7a");
            }
            if ("BETWEEN".equals(rule.getOperator())) {
                Object compareValue = rule.getCompareValue();
                if (!(compareValue instanceof List<?> valueList) || valueList.size() < 2) {
                    throw new IllegalArgumentException("BETWEEN \u64cd\u4f5c\u7b26\u9700\u8981\u4f20\u5165\u4e24\u4e2a\u6bd4\u8f83\u503c");
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
            throw new IllegalStateException("\u5e8f\u5217\u5316\u89c4\u5219\u6bd4\u8f83\u503c\u5931\u8d25", ex);
        }
    }

    private Object deserializeCompareValue(String compareValue) {
        if (trimToNull(compareValue) == null) {
            return null;
        }
        try {
            return objectMapper.readValue(compareValue, Object.class);
        } catch (Exception ex) {
            throw new IllegalStateException("\u53cd\u5e8f\u5217\u5316\u89c4\u5219\u6bd4\u8f83\u503c\u5931\u8d25", ex);
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
            return "\u81ea\u52a8\u5212\u5206";
        }
        return "\u63d0\u4f9b\u9009\u62e9";
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
            throw new IllegalArgumentException("\u8d39\u7528\u7c7b\u578b\u7f16\u7801\u5fc5\u987b\u4e3a 6 \u4f4d\u6216 8 \u4f4d\u6570\u5b57");
        }

        if (!EXPENSE_TYPE_INVOICE_MODES.contains(trimToEmpty(dto.getInvoiceFreeMode()))) {
            throw new IllegalArgumentException("\u662f\u5426\u514d\u7968\u914d\u7f6e\u4e0d\u5408\u6cd5");
        }
        if (!EXPENSE_TYPE_TAX_MODES.contains(trimToEmpty(dto.getTaxDeductionMode()))) {
            throw new IllegalArgumentException("\u7a0e\u989d\u62b5\u6263\u4e0e\u8f6c\u51fa\u914d\u7f6e\u4e0d\u5408\u6cd5");
        }
        if (!EXPENSE_TYPE_SEPARATION_MODES.contains(trimToEmpty(dto.getTaxSeparationMode()))) {
            throw new IllegalArgumentException("\u4ef7\u7a0e\u5206\u79bb\u89c4\u5219\u914d\u7f6e\u4e0d\u5408\u6cd5");
        }

        ProcessExpenseType duplicated = findExpenseTypeByCode(expenseCode);
        if (duplicated != null && (existing == null || !Objects.equals(duplicated.getId(), existing.getId()))) {
            throw new IllegalArgumentException("\u8d39\u7528\u7c7b\u578b\u7f16\u7801\u5df2\u5b58\u5728");
        }

        ProcessExpenseType parentExpenseType = null;
        if (expenseCode.length() == 8) {
            parentExpenseType = findExpenseTypeByCode(expenseCode.substring(0, 6));
            if (parentExpenseType == null) {
                throw new IllegalArgumentException("8 \u4f4d\u8d39\u7528\u7c7b\u578b\u7f16\u7801\u5fc5\u987b\u5148\u5b58\u5728\u5bf9\u5e94\u7684 6 \u4f4d\u7236\u7ea7\u7f16\u7801");
            }
        }

        if (existing != null && !Objects.equals(existing.getExpenseCode(), expenseCode)) {
            if (hasExpenseTypeChildren(existing.getId())) {
                throw new IllegalStateException("\u5f53\u524d\u8d39\u7528\u7c7b\u578b\u5b58\u5728\u5b50\u7ea7\u8282\u70b9\uff0c\u4e0d\u80fd\u4fee\u6539\u7f16\u7801");
            }
            if (isExpenseTypeReferenced(existing)) {
                throw new IllegalStateException("\u5f53\u524d\u8d39\u7528\u7c7b\u578b\u5df2\u88ab\u6a21\u677f\u5f15\u7528\uff0c\u4e0d\u80fd\u4fee\u6539\u7f16\u7801");
            }
        }

        Integer targetStatus = normalizeStatus(dto.getStatus());
        if (targetStatus == 1 && parentExpenseType != null && !Objects.equals(parentExpenseType.getStatus(), 1)) {
            throw new IllegalStateException("\u7236\u7ea7\u8d39\u7528\u7c7b\u578b\u672a\u542f\u7528\uff0c\u5b50\u7ea7\u4e0d\u80fd\u76f4\u63a5\u542f\u7528");
        }

        validateSelectableIds(normalizeIdList(dto.getScopeDeptIds()), loadValidDepartmentIdSet(), "\u90e8\u95e8");
        validateSelectableIds(normalizeIdList(dto.getScopeUserIds()), loadValidUserIdSet(), "\u4eba\u5458");
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
            throw new IllegalStateException("\u7236\u7ea7\u8d39\u7528\u7c7b\u578b\u672a\u542f\u7528\uff0c\u5b50\u7ea7\u4e0d\u80fd\u542f\u7528");
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
            throw new IllegalStateException("\u8d39\u7528\u7c7b\u578b\u4e0d\u5b58\u5728");
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
            String label = trimToNull(user.getName()) != null ? user.getName() : normalize(user.getUsername(), "\u672a\u547d\u540d\u7528\u6237");
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

    private Set<String> loadValidExpenseTypeCodeSet() {
        return loadEnabledExpenseTypes().stream()
                .map(ProcessExpenseType::getExpenseCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void validateSelectableIds(List<String> selectedIds, Set<String> validIds, String fieldName) {
        for (String selectedId : selectedIds) {
            if (!validIds.contains(selectedId)) {
                throw new IllegalArgumentException(fieldName + " \u9009\u62e9\u9879\u4e0d\u5b58\u5728: " + selectedId);
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
            throw new IllegalStateException("\u5e8f\u5217\u5316\u8303\u56f4\u6570\u636e\u5931\u8d25", ex);
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
            throw new IllegalStateException("\u53cd\u5e8f\u5217\u5316\u8303\u56f4\u6570\u636e\u5931\u8d25", ex);
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
