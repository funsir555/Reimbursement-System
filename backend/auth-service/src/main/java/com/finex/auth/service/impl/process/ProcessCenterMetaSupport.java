package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ProcessCenterNavItemVO;
import com.finex.auth.dto.ProcessCenterOverviewVO;
import com.finex.auth.dto.ProcessCenterSummaryVO;
import com.finex.auth.dto.ProcessExpenseDetailDesignSummaryVO;
import com.finex.auth.dto.ProcessExpenseTypeTreeVO;
import com.finex.auth.dto.ProcessFormDesignSummaryVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.dto.ProcessTemplateCardVO;
import com.finex.auth.dto.ProcessTemplateCategoryVO;
import com.finex.auth.dto.ProcessTemplateFormOptionsVO;
import com.finex.auth.dto.ProcessTemplateTypeVO;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseType;
import com.finex.auth.entity.ProcessTemplateCategory;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessTemplateCategoryMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.service.ProcessExpenseDetailDesignService;
import com.finex.auth.service.ProcessFlowDesignService;
import com.finex.auth.service.ProcessFormDesignService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ProcessCenterMetaSupport {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String TEMPLATE_STATUS_ENABLED = "ENABLED";
    private static final String TEMPLATE_STATUS_DRAFT = "DRAFT";
    private static final String TEMPLATE_STATUS_DELETED = "DELETED";

    private final ProcessTemplateCategoryMapper categoryMapper;
    private final ProcessDocumentTemplateMapper templateMapper;
    private final ProcessCustomArchiveDesignMapper customArchiveDesignMapper;
    private final ProcessExpenseTypeMapper processExpenseTypeMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;
    private final ProcessFormDesignService processFormDesignService;
    private final ProcessExpenseDetailDesignService processExpenseDetailDesignService;
    private final ProcessFlowDesignService processFlowDesignService;

    public ProcessCenterMetaSupport(
            ProcessTemplateCategoryMapper categoryMapper,
            ProcessDocumentTemplateMapper templateMapper,
            ProcessCustomArchiveDesignMapper customArchiveDesignMapper,
            ProcessExpenseTypeMapper processExpenseTypeMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            ProcessFormDesignService processFormDesignService,
            ProcessExpenseDetailDesignService processExpenseDetailDesignService,
            ProcessFlowDesignService processFlowDesignService
    ) {
        this.categoryMapper = categoryMapper;
        this.templateMapper = templateMapper;
        this.customArchiveDesignMapper = customArchiveDesignMapper;
        this.processExpenseTypeMapper = processExpenseTypeMapper;
        this.systemDepartmentMapper = systemDepartmentMapper;
        this.processFormDesignService = processFormDesignService;
        this.processExpenseDetailDesignService = processExpenseDetailDesignService;
        this.processFlowDesignService = processFlowDesignService;
    }

    public ProcessCenterOverviewVO getOverview() {
        List<ProcessTemplateCategory> categories = categoryMapper.selectList(
                Wrappers.<ProcessTemplateCategory>lambdaQuery()
                        .eq(ProcessTemplateCategory::getStatus, 1)
                        .orderByAsc(ProcessTemplateCategory::getSortOrder, ProcessTemplateCategory::getId)
        );
        List<ProcessDocumentTemplate> templates = templateMapper.selectList(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .in(ProcessDocumentTemplate::getPublishStatus, TEMPLATE_STATUS_ENABLED, TEMPLATE_STATUS_DRAFT)
                        .orderByAsc(ProcessDocumentTemplate::getSortOrder, ProcessDocumentTemplate::getId)
        );

        Map<String, ProcessTemplateCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(
                        ProcessTemplateCategory::getCategoryCode,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<String, String> formNameMap = processFormDesignService.listFormDesigns(null).stream()
                .collect(Collectors.toMap(
                        ProcessFormDesignSummaryVO::getFormCode,
                        ProcessFormDesignSummaryVO::getFormName,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<String, String> expenseDetailDesignNameMap = processExpenseDetailDesignService.listExpenseDetailDesigns().stream()
                .collect(Collectors.toMap(
                        ProcessExpenseDetailDesignSummaryVO::getDetailCode,
                        ProcessExpenseDetailDesignSummaryVO::getDetailName,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        ProcessCenterOverviewVO overview = new ProcessCenterOverviewVO();
        overview.setNavItems(buildNavItems());
        overview.setSummary(buildSummary(templates));
        overview.setCategories(buildCategoryCards(categories, templates, categoryMap, formNameMap, expenseDetailDesignNameMap));
        return overview;
    }

    public List<ProcessTemplateTypeVO> getTemplateTypes() {
        return List.of(
                templateType("report", "报销单", "费用报销", "适用于员工报销、差旅报销与团队费用归集等场景。", "blue"),
                templateType("application", "申请单", "业务申请", "适用于预算申请、付款申请、项目申请等事前流程。", "cyan"),
                templateType("loan", "借款单", "借支管理", "适用于备用金借支、项目借款及后续核销归还场景。", "orange"),
                templateType("contract", "合同单", "合同管理", "适用于合同申请、合同评审、签订流转及后续合同管理场景。", "emerald")
        );
    }

    public ProcessTemplateFormOptionsVO getFormOptions(String templateType) {
        ProcessTemplateFormOptionsVO options = new ProcessTemplateFormOptionsVO();
        options.setTemplateType(templateType);
        options.setTemplateTypeLabel(resolveTemplateTypeLabel(templateType));
        options.setCategoryOptions(loadTemplateCategoryOptions());
        options.setNumberingRulePreview("FX+年+月+日+4位数字（如：FX202503251234）");
        options.setFormDesignOptions(loadFormDesignOptions(templateType));
        options.setExpenseDetailDesignOptions(loadExpenseDetailDesignOptions(templateType));
        options.setExpenseDetailModeOptions(loadExpenseDetailModeOptions(templateType));
        options.setApprovalFlows(processFlowDesignService.listPublishedFlowOptions());
        options.setPrintModes(List.of(
                option("默认打印模板", "default-print"),
                option("横版摘要模板", "landscape-summary"),
                option("财务归档模板", "finance-archive")
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
        options.setDepartmentOptions(loadDepartmentOptions());
        options.setAiAuditModes(List.of(
                option("关闭 AI 审核", "disabled"),
                option("标准风险识别", "standard"),
                option("严格风险识别", "strict")
        ));
        List<ProcessFormOptionVO> archiveOptions = loadEnabledArchiveOptions();
        options.setTagOptions(archiveOptions);
        options.setInstallmentOptions(archiveOptions);
        return options;
    }

    private List<ProcessCenterNavItemVO> buildNavItems() {
        return List.of(
                navItem("document-flow", "单据与流程", "查看模板、流程与绑定关系的整体概览"),
                navItem("form-design", "费用表单", "维护可被单据模板绑定的业务表单设计"),
                navItem("approval-flow", "审批流程", "维护模板使用的审批流程与版本配置"),
                navItem("expense-detail-form", "费用明细表单", "维护报销模板专用的费用明细子表单"),
                navItem("custom-archive", "自定义档案", "维护标签、分期付款等业务配置档案"),
                navItem("user-group", "用户组", "维护审批流可复用的用户组与管理范围"),
                navItem("expense-type", "费用类型", "维护费用类型树和发票税务配置")
        );
    }

    private ProcessCenterSummaryVO buildSummary(List<ProcessDocumentTemplate> templates) {
        ProcessCenterSummaryVO summary = new ProcessCenterSummaryVO();
        summary.setTotalTemplates(templates.size());
        summary.setEnabledTemplates((int) templates.stream().filter(item -> Objects.equals(item.getEnabled(), 1)).count());
        summary.setDraftTemplates((int) templates.stream().filter(item -> TEMPLATE_STATUS_DRAFT.equals(item.getPublishStatus())).count());
        summary.setAiAuditTemplates((int) templates.stream()
                .filter(item -> !"disabled".equalsIgnoreCase(normalize(item.getAiAuditMode(), "disabled")))
                .count());
        return summary;
    }

    private List<ProcessTemplateCategoryVO> buildCategoryCards(
            List<ProcessTemplateCategory> categories,
            List<ProcessDocumentTemplate> templates,
            Map<String, ProcessTemplateCategory> categoryMap,
            Map<String, String> formNameMap,
            Map<String, String> expenseDetailDesignNameMap
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
            card.setDescription(normalize(category.getCategoryDescription(), "维护该分类下的流程模板"));
            card.setTemplateCount(categoryTemplates.size());
            card.setTemplates(buildTemplateCards(categoryTemplates, category.getCategoryName(), formNameMap, expenseDetailDesignNameMap));
            result.add(card);
        }

        for (Map.Entry<String, List<ProcessDocumentTemplate>> entry : groupedTemplates.entrySet()) {
            if (categoryMap.containsKey(entry.getKey())) {
                continue;
            }
            ProcessTemplateCategoryVO card = new ProcessTemplateCategoryVO();
            card.setCode(entry.getKey());
            card.setName(entry.getKey());
            card.setDescription("未归类模板");
            card.setTemplateCount(entry.getValue().size());
            card.setTemplates(buildTemplateCards(entry.getValue(), entry.getKey(), formNameMap, expenseDetailDesignNameMap));
            result.add(card);
        }
        return result;
    }

    private List<ProcessTemplateCardVO> buildTemplateCards(
            List<ProcessDocumentTemplate> templates,
            String categoryName,
            Map<String, String> formNameMap,
            Map<String, String> expenseDetailDesignNameMap
    ) {
        return templates.stream().map(template -> {
            String flowCode = trimToNull(template.getApprovalFlow());
            String formCode = trimToNull(template.getFormDesignCode());
            String expenseDetailDesignCode = trimToNull(template.getExpenseDetailDesignCode());
            ProcessTemplateCardVO card = new ProcessTemplateCardVO();
            card.setId(template.getId());
            card.setTemplateCode(template.getTemplateCode());
            card.setName(template.getTemplateName());
            card.setTemplateTypeCode(template.getTemplateType());
            card.setTemplateType(normalize(template.getTemplateTypeLabel(), resolveTemplateTypeLabel(template.getTemplateType())));
            card.setBusinessDomain(categoryName);
            card.setDescription(normalize(template.getTemplateDescription(), "维护该模板的配置说明"));
            card.setHighlights(splitHighlights(template.getHighlights()));
            card.setFlowCode(flowCode);
            card.setFlowName(normalize(template.getFlowName(), "未设置审批流程"));
            card.setFormCode(formCode);
            card.setFormName(normalize(formNameMap.get(formCode), "未绑定表单"));
            card.setExpenseDetailDesignCode(expenseDetailDesignCode);
            card.setExpenseDetailDesignName(normalize(expenseDetailDesignNameMap.get(expenseDetailDesignCode), "未绑定明细表单"));
            card.setStatus(template.getPublishStatus());
            card.setStatusLabel(resolveTemplateStatusLabel(template.getPublishStatus()));
            card.setUpdatedAt(formatDateTime(template.getUpdatedAt()));
            card.setOwner(normalize(template.getOwnerName(), "流程管理员"));
            card.setColor(resolveColor(template.getIconColor()));
            return card;
        }).toList();
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
                option("员工报销", "employee-expense"),
                option("对公付款", "enterprise-payment"),
                option("业务申请", "business-application")
        );
    }

    private List<ProcessFormOptionVO> loadFormDesignOptions(String templateType) {
        return processFormDesignService.listFormDesignOptions(templateType);
    }

    private List<ProcessExpenseDetailDesignSummaryVO> loadExpenseDetailDesignOptions(String templateType) {
        return Objects.equals(normalize(templateType, "report"), "report")
                ? processExpenseDetailDesignService.listExpenseDetailDesigns()
                : Collections.emptyList();
    }

    private List<ProcessFormOptionVO> loadExpenseDetailModeOptions(String templateType) {
        if (!Objects.equals(normalize(templateType, "report"), "report")) {
            return Collections.emptyList();
        }
        return List.of(
                option("预付未到票", "PREPAY_UNBILLED"),
                option("到票全部支付", "INVOICE_FULL_PAYMENT")
        );
    }

    private List<ProcessExpenseTypeTreeVO> loadEnabledExpenseTypeTree() {
        return buildExpenseTypeTree(processExpenseTypeMapper.selectList(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .eq(ProcessExpenseType::getStatus, 1)
                        .orderByAsc(ProcessExpenseType::getExpenseCode, ProcessExpenseType::getId)
        ));
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

    private List<ProcessFormOptionVO> loadDepartmentOptions() {
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .select(SystemDepartment::getId, SystemDepartment::getDeptCode, SystemDepartment::getDeptName, SystemDepartment::getParentId)
                        .eq(SystemDepartment::getStatus, 1)
                        .orderByAsc(SystemDepartment::getSortOrder, SystemDepartment::getId)
        ).stream().map(this::departmentOption).toList();
    }

    private ProcessFormOptionVO departmentOption(SystemDepartment department) {
        String value = String.valueOf(department.getId());
        ProcessFormOptionVO option = new ProcessFormOptionVO();
        option.setValue(value);
        option.setCode(department.getDeptCode());
        option.setName(department.getDeptName());
        option.setParentValue(department.getParentId() == null ? null : String.valueOf(department.getParentId()));
        option.setLabel(formatDepartmentLabel(department.getDeptCode(), department.getDeptName(), value));
        return option;
    }

    private List<ProcessFormOptionVO> loadEnabledArchiveOptions() {
        return customArchiveDesignMapper.selectList(
                Wrappers.<ProcessCustomArchiveDesign>lambdaQuery()
                        .eq(ProcessCustomArchiveDesign::getStatus, 1)
                        .orderByDesc(ProcessCustomArchiveDesign::getUpdatedAt, ProcessCustomArchiveDesign::getId)
        ).stream().map(archive -> option(archive.getArchiveName(), archive.getArchiveCode())).toList();
    }

    private String splitDefaultHighlights() {
        return "暂无亮点";
    }

    private List<String> splitHighlights(String highlights) {
        if (trimToNull(highlights) == null) {
            return List.of(splitDefaultHighlights());
        }
        return List.of(highlights.split("\\|")).stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String resolveTemplateTypeLabel(String templateType) {
        return switch (normalize(templateType, "report")) {
            case "application" -> "申请单";
            case "loan" -> "借款单";
            case "contract" -> "合同单";
            default -> "报销单";
        };
    }

    private String resolveTemplateStatusLabel(String status) {
        if (TEMPLATE_STATUS_ENABLED.equals(status)) {
            return "已启用";
        }
        if (TEMPLATE_STATUS_DRAFT.equals(status)) {
            return "草稿";
        }
        if (TEMPLATE_STATUS_DELETED.equals(status)) {
            return "已删除";
        }
        return "草稿";
    }

    private String resolveColor(String iconColor) {
        return switch (normalize(iconColor, "blue")) {
            case "cyan" -> "linear-gradient(135deg, #0891b2 0%, #67e8f9 100%)";
            case "orange" -> "linear-gradient(135deg, #ea580c 0%, #fdba74 100%)";
            case "green" -> "linear-gradient(135deg, #15803d 0%, #86efac 100%)";
            default -> "linear-gradient(135deg, #2563eb 0%, #60a5fa 100%)";
        };
    }

    private String normalize(String value, String defaultValue) {
        String normalized = trimToNull(value);
        return normalized == null ? defaultValue : normalized;
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

    private String formatDepartmentLabel(String code, String name, String fallback) {
        String normalizedCode = trimToNull(code);
        String normalizedName = trimToNull(name);
        if (normalizedCode != null && normalizedName != null) {
            return normalizedCode + "  " + normalizedName;
        }
        return normalizedName != null ? normalizedName : normalizedCode != null ? normalizedCode : fallback;
    }
}
