package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ProcessCenterNavItemVO;
import com.finex.auth.dto.ProcessCenterOverviewVO;
import com.finex.auth.dto.ProcessCenterSummaryVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.dto.ProcessTemplateCardVO;
import com.finex.auth.dto.ProcessTemplateCategoryVO;
import com.finex.auth.dto.ProcessTemplateFormOptionsVO;
import com.finex.auth.dto.ProcessTemplateSaveDTO;
import com.finex.auth.dto.ProcessTemplateSaveResultVO;
import com.finex.auth.dto.ProcessTemplateTypeVO;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessTemplateCategory;
import com.finex.auth.entity.ProcessTemplateScope;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessTemplateCategoryMapper;
import com.finex.auth.mapper.ProcessTemplateScopeMapper;
import com.finex.auth.service.ProcessManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcessManagementServiceImpl implements ProcessManagementService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String HIGHLIGHT_SEPARATOR = "|";

    private final ProcessTemplateCategoryMapper categoryMapper;
    private final ProcessDocumentTemplateMapper templateMapper;
    private final ProcessTemplateScopeMapper scopeMapper;

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
        options.setCategoryOptions(List.of(
                option("员工费用类", "employee-expense"),
                option("企业往来类", "enterprise-payment"),
                option("事项申请类", "business-application")
        ));
        options.setNumberingRules(List.of(
                option("按年度流水号", "year-sequence"),
                option("按部门月份流水号", "department-month-sequence"),
                option("自定义前缀编码", "custom-prefix")
        ));
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
        options.setTravelForms(List.of(
                option("标准出差行程表", "travel-standard"),
                option("国际差旅行程表", "travel-global"),
                option("项目走访行程表", "travel-project")
        ));
        options.setAllocationForms(List.of(
                option("默认分摊表", "allocation-default"),
                option("项目分摊表", "allocation-project"),
                option("部门分摊表", "allocation-department")
        ));
        options.setExpenseTypes(List.of(
                option("差旅费", "travel"),
                option("招待费", "entertainment"),
                option("办公费", "office"),
                option("市场活动费", "marketing"),
                option("培训费", "training")
        ));
        options.setAiAuditModes(List.of(
                option("关闭 AI 审核", "disabled"),
                option("标准风险识别", "standard"),
                option("严格风险识别", "strict")
        ));
        options.setScopeOptions(List.of(
                option("限定部门使用", "department"),
                option("限定岗位使用", "position"),
                option("限定费用类型使用", "expense-type"),
                option("限制金额区间", "amount-range")
        ));
        options.setTagOptions(List.of(
                option("高频模板", "high-frequency"),
                option("对公业务", "public-payment"),
                option("AI 审核", "ai-audit"),
                option("支持分期付款", "split-payment")
        ));
        return options;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessTemplateSaveResultVO saveTemplate(ProcessTemplateSaveDTO dto, String operatorName) {
        String categoryCode = normalize(dto.getCategory(), "employee-expense");
        String templateType = normalize(dto.getTemplateType(), "report");
        boolean enabled = dto.getEnabled() == null || dto.getEnabled();

        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateCode(buildTemplateCode(templateType));
        template.setTemplateName(dto.getTemplateName().trim());
        template.setTemplateType(templateType);
        template.setTemplateTypeLabel(resolveTemplateTypeLabel(templateType));
        template.setCategoryCode(categoryCode);
        template.setTemplateDescription(resolveDescription(dto));
        template.setNumberingRule(normalize(dto.getNumberingRule(), "year-sequence"));
        template.setIconColor(normalize(dto.getIconColor(), "blue"));
        template.setEnabled(enabled ? 1 : 0);
        template.setPublishStatus(enabled ? "ENABLED" : "DRAFT");
        template.setPrintMode(normalize(dto.getPrintMode(), "default-print"));
        template.setApprovalFlow(normalize(dto.getApprovalFlow(), "normal-expense-flow"));
        template.setFlowName(resolveFlowLabel(template.getApprovalFlow()));
        template.setPaymentMode(normalize(dto.getPaymentMode(), "none"));
        template.setSplitPayment(Boolean.TRUE.equals(dto.getSplitPayment()) ? 1 : 0);
        template.setTravelForm(normalize(dto.getTravelForm(), "travel-standard"));
        template.setAllocationForm(normalize(dto.getAllocationForm(), "allocation-default"));
        template.setAiAuditMode(normalize(dto.getAiAuditMode(), "disabled"));
        template.setRelationRemark(dto.getRelationRemark());
        template.setValidationRemark(dto.getValidationRemark());
        template.setInstallmentRemark(dto.getInstallmentRemark());
        template.setHighlights(String.join(HIGHLIGHT_SEPARATOR, buildHighlights(dto)));
        template.setOwnerName(normalize(operatorName, "流程管理员"));
        template.setSortOrder(nextSortOrder(categoryCode));

        templateMapper.insert(template);

        saveScopeItems(template.getId(), "EXPENSE_TYPE", dto.getExpenseTypes(), expenseTypeLabelMap());
        saveScopeItems(template.getId(), "SCOPE_OPTION", dto.getScopeOptions(), scopeLabelMap());
        saveScopeItems(template.getId(), "TAG_OPTION", dto.getTagOptions(), tagLabelMap());

        ProcessTemplateSaveResultVO result = new ProcessTemplateSaveResultVO();
        result.setId(template.getId());
        result.setTemplateCode(template.getTemplateCode());
        result.setTemplateName(template.getTemplateName());
        result.setStatus(template.getPublishStatus());
        return result;
    }

    private List<ProcessCenterNavItemVO> buildNavItems() {
        return List.of(
                navItem("role-permission", "角色与权限", "配置流程维护角色与可见范围"),
                navItem("user-group", "用户组", "维护流程管理员和业务用户组"),
                navItem("custom-archive", "自定义档案", "维护业务档案与扩展字段"),
                navItem("currency-rate", "币种与汇率", "配置多币种与折算汇率"),
                navItem("document-flow", "单据与流程", "维护单据模板、流程和付款规则"),
                navItem("expense-type", "费用类型", "配置费用科目和费用口径"),
                navItem("travel-form", "行程表单", "配置出差与行程采集表单"),
                navItem("allocation-form", "分摊表单", "配置费用分摊规则与表单"),
                navItem("ai-audit", "AI审核", "配置 AI 风险审核策略")
        );
    }

    private ProcessCenterSummaryVO buildSummary(List<ProcessDocumentTemplate> templates) {
        int totalTemplates = templates.size();
        int enabledTemplates = (int) templates.stream()
                .filter(template -> Integer.valueOf(1).equals(template.getEnabled()))
                .count();
        int draftTemplates = (int) templates.stream()
                .filter(template -> "DRAFT".equalsIgnoreCase(template.getPublishStatus()))
                .count();
        int aiAuditTemplates = (int) templates.stream()
                .filter(template -> template.getAiAuditMode() != null && !"disabled".equalsIgnoreCase(template.getAiAuditMode()))
                .count();

        ProcessCenterSummaryVO summary = new ProcessCenterSummaryVO();
        summary.setTotalTemplates(totalTemplates);
        summary.setEnabledTemplates(enabledTemplates);
        summary.setDraftTemplates(draftTemplates);
        summary.setAiAuditTemplates(aiAuditTemplates);
        return summary;
    }

    private List<ProcessTemplateCategoryVO> buildCategoryCards(
            List<ProcessTemplateCategory> categories,
            List<ProcessDocumentTemplate> templates,
            Map<String, ProcessTemplateCategory> categoryMap
    ) {
        Map<String, List<ProcessDocumentTemplate>> groupedTemplates = templates.stream()
                .collect(Collectors.groupingBy(
                        ProcessDocumentTemplate::getCategoryCode,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return categories.stream()
                .map(category -> {
                    List<ProcessTemplateCardVO> cards = groupedTemplates
                            .getOrDefault(category.getCategoryCode(), List.of())
                            .stream()
                            .map(template -> toCard(template, categoryMap))
                            .toList();

                    ProcessTemplateCategoryVO vo = new ProcessTemplateCategoryVO();
                    vo.setCode(category.getCategoryCode());
                    vo.setName(category.getCategoryName());
                    vo.setDescription(category.getCategoryDescription());
                    vo.setTemplateCount(cards.size());
                    vo.setTemplates(cards);
                    return vo;
                })
                .filter(category -> !category.getTemplates().isEmpty())
                .toList();
    }

    private ProcessTemplateCardVO toCard(
            ProcessDocumentTemplate template,
            Map<String, ProcessTemplateCategory> categoryMap
    ) {
        ProcessTemplateCategory category = categoryMap.get(template.getCategoryCode());

        ProcessTemplateCardVO card = new ProcessTemplateCardVO();
        card.setId(template.getId());
        card.setTemplateCode(template.getTemplateCode());
        card.setName(template.getTemplateName());
        card.setTemplateType(resolveTemplateTypeLabel(template.getTemplateType()));
        card.setBusinessDomain(category == null ? template.getCategoryCode() : category.getCategoryName());
        card.setDescription(template.getTemplateDescription());
        card.setHighlights(splitHighlights(template.getHighlights()));
        card.setFlowName(normalize(template.getFlowName(), resolveFlowLabel(template.getApprovalFlow())));
        card.setUpdatedAt(template.getUpdatedAt() == null ? "" : template.getUpdatedAt().format(DATE_TIME_FORMATTER));
        card.setOwner(normalize(template.getOwnerName(), "流程管理员"));
        card.setColor(resolveColor(template.getIconColor()));
        return card;
    }

    private void saveScopeItems(Long templateId, String optionType, List<String> values, Map<String, String> labelMap) {
        if (values == null || values.isEmpty()) {
            return;
        }

        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            ProcessTemplateScope scope = new ProcessTemplateScope();
            scope.setTemplateId(templateId);
            scope.setOptionType(optionType);
            scope.setOptionCode(value);
            scope.setOptionLabel(labelMap.getOrDefault(value, value));
            scope.setSortOrder(i + 1);
            scopeMapper.insert(scope);
        }
    }

    private int nextSortOrder(String categoryCode) {
        List<ProcessDocumentTemplate> templates = templateMapper.selectList(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .eq(ProcessDocumentTemplate::getCategoryCode, categoryCode)
                        .orderByDesc(ProcessDocumentTemplate::getSortOrder)
                        .last("limit 1")
        );

        if (templates.isEmpty() || templates.get(0).getSortOrder() == null) {
            return 100;
        }
        return templates.get(0).getSortOrder() + 10;
    }

    private List<String> buildHighlights(ProcessTemplateSaveDTO dto) {
        List<String> highlights = new ArrayList<>();
        highlights.add("支持移动端提单");

        if (!"none".equalsIgnoreCase(normalize(dto.getPaymentMode(), "none"))) {
            highlights.add("联动付款单");
        }
        if (Boolean.TRUE.equals(dto.getSplitPayment())) {
            highlights.add("支持分期付款");
        }
        if (!"disabled".equalsIgnoreCase(normalize(dto.getAiAuditMode(), "disabled"))) {
            highlights.add("AI 审核");
        }

        while (highlights.size() < 3) {
            highlights.add("标准审批链路");
        }
        return highlights.stream().distinct().limit(3).toList();
    }

    private List<String> splitHighlights(String highlights) {
        if (highlights == null || highlights.isBlank()) {
            return List.of("支持移动端提单", "标准审批链路");
        }
        return Arrays.stream(highlights.split("\\|"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private String resolveDescription(ProcessTemplateSaveDTO dto) {
        if (dto.getTemplateDescription() != null && !dto.getTemplateDescription().isBlank()) {
            return dto.getTemplateDescription().trim();
        }
        return "适用于" + resolveTemplateTypeLabel(dto.getTemplateType()) + "场景，可继续补充流程与业务规则。";
    }

    private String resolveTemplateTypeLabel(String templateType) {
        return switch (templateType) {
            case "application" -> "申请单";
            case "loan" -> "借款单";
            default -> "报销单";
        };
    }

    private String resolveFlowLabel(String approvalFlow) {
        return switch (approvalFlow) {
            case "public-payment-flow" -> "对公付款流程";
            case "loan-return-flow" -> "借款与归还流程";
            default -> "标准报销流程";
        };
    }

    private String resolveColor(String iconColor) {
        return switch (iconColor) {
            case "cyan" -> "linear-gradient(135deg, #0891b2 0%, #67e8f9 100%)";
            case "orange" -> "linear-gradient(135deg, #ea580c 0%, #fdba74 100%)";
            default -> "linear-gradient(135deg, #2563eb 0%, #60a5fa 100%)";
        };
    }

    private String buildTemplateCode(String templateType) {
        String prefix = switch (templateType) {
            case "application" -> "APP";
            case "loan" -> "LOAN";
            default -> "EXP";
        };
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 999);
    }

    private String normalize(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private Map<String, String> expenseTypeLabelMap() {
        return List.of(
                option("差旅费", "travel"),
                option("招待费", "entertainment"),
                option("办公费", "office"),
                option("市场活动费", "marketing"),
                option("培训费", "training")
        ).stream().collect(Collectors.toMap(ProcessFormOptionVO::getValue, ProcessFormOptionVO::getLabel));
    }

    private Map<String, String> scopeLabelMap() {
        return List.of(
                option("限定部门使用", "department"),
                option("限定岗位使用", "position"),
                option("限定费用类型使用", "expense-type"),
                option("限制金额区间", "amount-range")
        ).stream().collect(Collectors.toMap(ProcessFormOptionVO::getValue, ProcessFormOptionVO::getLabel));
    }

    private Map<String, String> tagLabelMap() {
        return List.of(
                option("高频模板", "high-frequency"),
                option("对公业务", "public-payment"),
                option("AI 审核", "ai-audit"),
                option("支持分期付款", "split-payment")
        ).stream().collect(Collectors.toMap(ProcessFormOptionVO::getValue, ProcessFormOptionVO::getLabel));
    }

    private ProcessCenterNavItemVO navItem(String key, String label, String tip) {
        ProcessCenterNavItemVO navItem = new ProcessCenterNavItemVO();
        navItem.setKey(key);
        navItem.setLabel(label);
        navItem.setTip(tip);
        return navItem;
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
}
