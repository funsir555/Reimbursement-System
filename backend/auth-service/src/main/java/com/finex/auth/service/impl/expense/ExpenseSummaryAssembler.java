package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessCustomArchiveItem;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessTemplateScope;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessCustomArchiveItemMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessTemplateScopeMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseSummaryAssembler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String UNDERTAKE_DEPARTMENT_COMPONENT_CODE = "undertake-department";
    private static final String PAYMENT_COMPANY_COMPONENT_CODE = "payment-company";
    private static final String PAYEE_COMPONENT_CODE = "payee";
    private static final String COUNTERPARTY_COMPONENT_CODE = "counterparty";
    private static final String PERSONAL_PAYEE_VALUE_PREFIX = "PERSONAL_PAYEE:";
    private static final String CONTROL_TYPE_DATE = "DATE";
    private static final Charset LEGACY_GARBLED_CHARSET = Charset.forName("GBK");
    private static final Set<String> PAYMENT_DATE_LABELS = Set.of("\u652f\u4ed8\u65e5\u671f", "\u4ed8\u6b3e\u65e5\u671f");
    private static final Set<String> PAYMENT_DATE_LEGACY_LABEL_ALIASES = buildLegacyUtf8AsGbkLabels(PAYMENT_DATE_LABELS);
    private static final String TEMPLATE_SCOPE_TYPE_TAG_ARCHIVE = "TAG_ARCHIVE";
    private static final String DOCUMENT_STATUS_APPROVED = "APPROVED";
    private static final String DOCUMENT_STATUS_REJECTED = "REJECTED";
    private static final String DOCUMENT_STATUS_EXCEPTION = "EXCEPTION";
    private static final String DOCUMENT_STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String DOCUMENT_STATUS_PAYING = "PAYING";
    private static final String DOCUMENT_STATUS_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    private static final String DOCUMENT_STATUS_PAYMENT_FINISHED = "PAYMENT_FINISHED";
    private static final String DOCUMENT_STATUS_PAYMENT_EXCEPTION = "PAYMENT_EXCEPTION";

    private final ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    private final ProcessDocumentTemplateMapper templateMapper;
    private final ProcessTemplateScopeMapper processTemplateScopeMapper;
    private final ProcessCustomArchiveDesignMapper customArchiveDesignMapper;
    private final ProcessCustomArchiveItemMapper customArchiveItemMapper;
    private final UserMapper userMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final FinanceVendorMapper financeVendorMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;
    private final ObjectMapper objectMapper;

    public List<ExpenseSummaryVO> toExpenseSummaries(List<ProcessDocumentInstance> instances) {
        return instances.isEmpty() ? Collections.emptyList() : toExpenseSummariesInternal(instances);
    }

    public List<ExpenseApprovalPendingItemVO> toPendingItems(List<ProcessDocumentTask> tasks, Map<String, ProcessDocumentInstance> instanceMap) {
        if (tasks.isEmpty() || instanceMap.isEmpty()) {
            return Collections.emptyList();
        }
        SummaryEnrichmentData enrichmentData = buildSummaryEnrichment(new ArrayList<>(instanceMap.values()));
        return tasks.stream().map(task -> toPendingItem(task, instanceMap.get(task.getDocumentCode()), enrichmentData)).toList();
    }


    private List<ExpenseSummaryVO> toExpenseSummariesInternal(List<ProcessDocumentInstance> instances) {
        SummaryEnrichmentData enrichmentData = buildSummaryEnrichment(instances);
        return instances.stream().map(instance -> toExpenseSummary(instance, enrichmentData)).toList();
    }

    private ExpenseSummaryVO toExpenseSummary(ProcessDocumentInstance instance, SummaryEnrichmentData enrichmentData) {
        ExpenseSummaryVO summary = new ExpenseSummaryVO();
        SummaryMetadata metadata = enrichmentData.metadata(instance.getDocumentCode());
        String statusLabel = resolveStatusLabel(instance.getStatus());
        summary.setDocumentCode(instance.getDocumentCode());
        summary.setNo(instance.getDocumentCode());
        summary.setType(trimToNull(instance.getTemplateName()) != null ? instance.getTemplateName() : resolveTemplateTypeLabel(instance.getTemplateType(), null));
        summary.setReason(trimToNull(instance.getDocumentReason()) != null ? instance.getDocumentReason() : defaultReason(instance.getDocumentTitle()));
        summary.setDocumentTitle(instance.getDocumentTitle());
        summary.setDocumentReason(instance.getDocumentReason());
        summary.setSubmitterName(instance.getSubmitterName());
        summary.setSubmitterDeptName(metadata.submitterDeptName());
        summary.setTemplateName(instance.getTemplateName());
        summary.setTemplateType(instance.getTemplateType());
        summary.setTemplateTypeLabel(resolveTemplateTypeLabel(instance.getTemplateType(), readMap(instance.getTemplateSnapshotJson()).get("templateTypeLabel") == null
                ? null
                : String.valueOf(readMap(instance.getTemplateSnapshotJson()).get("templateTypeLabel"))));
        summary.setCurrentNodeName(instance.getCurrentNodeName());
        summary.setDocumentStatus(instance.getStatus());
        summary.setDocumentStatusLabel(statusLabel);
        summary.setAmount(defaultDecimal(instance.getTotalAmount()));
        summary.setDate(instance.getCreatedAt() == null ? "" : instance.getCreatedAt().format(DATE_FORMATTER));
        summary.setStatus(statusLabel);
        summary.setSubmittedAt(formatTime(instance.getCreatedAt()));
        summary.setPaymentDate(metadata.paymentDate());
        summary.setPaymentCompanyName(metadata.paymentCompanyName());
        summary.setPayeeName(metadata.payeeName());
        summary.setCounterpartyName(metadata.counterpartyName());
        summary.setUndertakeDepartmentNames(metadata.undertakeDepartmentNames());
        summary.setTagNames(metadata.tagNames());
        return summary;
    }

    private ExpenseApprovalPendingItemVO toPendingItem(
            ProcessDocumentTask task,
            ProcessDocumentInstance instance,
            SummaryEnrichmentData enrichmentData
    ) {
        ExpenseApprovalPendingItemVO item = new ExpenseApprovalPendingItemVO();
        SummaryMetadata metadata = instance == null ? SummaryMetadata.empty() : enrichmentData.metadata(task.getDocumentCode());
        item.setTaskId(task.getId());
        item.setDocumentCode(task.getDocumentCode());
        item.setDocumentTitle(instance == null ? "" : instance.getDocumentTitle());
        item.setDocumentReason(instance == null ? "" : instance.getDocumentReason());
        item.setTemplateName(instance == null ? "" : instance.getTemplateName());
        item.setTemplateType(instance == null ? null : instance.getTemplateType());
        item.setTemplateTypeLabel(instance == null ? null : resolveTemplateTypeLabel(instance.getTemplateType(), readMap(instance.getTemplateSnapshotJson()).get("templateTypeLabel") == null
                ? null
                : String.valueOf(readMap(instance.getTemplateSnapshotJson()).get("templateTypeLabel"))));
        item.setSubmitterName(instance == null ? "" : instance.getSubmitterName());
        item.setSubmitterDeptName(metadata.submitterDeptName());
        item.setAmount(instance == null ? BigDecimal.ZERO : defaultDecimal(instance.getTotalAmount()));
        item.setNodeKey(task.getNodeKey());
        item.setNodeName(task.getNodeName());
        item.setStatus(task.getStatus());
        item.setDocumentStatus(instance == null ? null : instance.getStatus());
        item.setDocumentStatusLabel(instance == null ? null : resolveStatusLabel(instance.getStatus()));
        item.setSubmittedAt(instance == null ? null : formatTime(instance.getCreatedAt()));
        item.setPaymentDate(metadata.paymentDate());
        item.setPaymentCompanyName(metadata.paymentCompanyName());
        item.setPayeeName(metadata.payeeName());
        item.setCounterpartyName(metadata.counterpartyName());
        item.setUndertakeDepartmentNames(metadata.undertakeDepartmentNames());
        item.setTagNames(metadata.tagNames());
        item.setTaskCreatedAt(formatTime(task.getCreatedAt()));
        return item;
    }

    private SummaryEnrichmentData buildSummaryEnrichment(List<ProcessDocumentInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return SummaryEnrichmentData.empty();
        }

        List<String> documentCodes = instances.stream()
                .map(ProcessDocumentInstance::getDocumentCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, List<ProcessDocumentExpenseDetail>> expenseDetailMap = documentCodes.isEmpty()
                ? Collections.emptyMap()
                : processDocumentExpenseDetailMapper.selectList(
                Wrappers.<ProcessDocumentExpenseDetail>lambdaQuery()
                        .in(ProcessDocumentExpenseDetail::getDocumentCode, documentCodes)
                        .orderByAsc(ProcessDocumentExpenseDetail::getSortOrder, ProcessDocumentExpenseDetail::getId)
        ).stream().collect(Collectors.groupingBy(
                ProcessDocumentExpenseDetail::getDocumentCode,
                LinkedHashMap::new,
                Collectors.toList()
        ));

        List<String> templateCodes = instances.stream()
                .map(ProcessDocumentInstance::getTemplateCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, ProcessDocumentTemplate> templateMap = templateCodes.isEmpty()
                ? Collections.emptyMap()
                : templateMapper.selectList(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .in(ProcessDocumentTemplate::getTemplateCode, templateCodes)
        ).stream().collect(Collectors.toMap(
                ProcessDocumentTemplate::getTemplateCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        Map<String, String> tagArchiveCodeByTemplateCode = loadTagArchiveCodeByTemplateCode(templateMap);

        Map<String, SummaryDraft> draftMap = new LinkedHashMap<>();
        Set<Long> userIds = new LinkedHashSet<>();
        Set<String> companyIds = new LinkedHashSet<>();
        Set<String> vendorCodes = new LinkedHashSet<>();
        Set<String> departmentIds = new LinkedHashSet<>();
        Set<String> archiveCodes = new LinkedHashSet<>();

        for (ProcessDocumentInstance instance : instances) {
            Map<String, Object> formData = readMap(instance.getFormDataJson());
            Map<String, Object> schema = readSchema(instance.getFormSchemaSnapshotJson());
            String documentCode = instance.getDocumentCode();
            List<ProcessDocumentExpenseDetail> expenseDetails = expenseDetailMap.getOrDefault(documentCode, Collections.emptyList());
            String tagArchiveCode = tagArchiveCodeByTemplateCode.get(instance.getTemplateCode());
            SummaryDraft draft = new SummaryDraft();
            draft.setDocumentCode(documentCode);
            draft.setPaymentCompanyId(extractFirstBusinessComponentValue(schema, formData, PAYMENT_COMPANY_COMPONENT_CODE));
            draft.setPayeeValue(extractFirstBusinessComponentValue(schema, formData, PAYEE_COMPONENT_CODE));
            draft.setCounterpartyValue(extractFirstBusinessComponentValue(schema, formData, COUNTERPARTY_COMPONENT_CODE));
            draft.setPaymentDate(extractPaymentDate(schema, formData));
            draft.setUndertakeDepartmentIds(resolveUndertakeDeptIdsFromSnapshots(schema, formData, expenseDetails));
            draft.setTagArchiveCode(tagArchiveCode);
            draft.setTagValues(tagArchiveCode == null ? Collections.emptyList() : extractArchiveValues(schema, formData, tagArchiveCode));
            draftMap.put(documentCode, draft);

            if (instance.getSubmitterUserId() != null) {
                userIds.add(instance.getSubmitterUserId());
            }
            if (draft.getPaymentCompanyId() != null) {
                companyIds.add(draft.getPaymentCompanyId());
            }
            collectPartyLookupIds(draft.getPayeeValue(), userIds, vendorCodes);
            collectVendorCode(draft.getCounterpartyValue(), vendorCodes);
            departmentIds.addAll(draft.getUndertakeDepartmentIds());
            if (tagArchiveCode != null) {
                archiveCodes.add(tagArchiveCode);
            }
        }

        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .in(User::getId, userIds)
        ).stream().collect(Collectors.toMap(
                User::getId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        userMap.values().stream()
                .map(User::getDeptId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .forEach(departmentIds::add);

        Map<String, SystemCompany> companyMap = companyIds.isEmpty()
                ? Collections.emptyMap()
                : systemCompanyMapper.selectList(
                Wrappers.<SystemCompany>lambdaQuery()
                        .in(SystemCompany::getCompanyId, companyIds)
        ).stream().collect(Collectors.toMap(
                SystemCompany::getCompanyId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        Map<String, FinanceVendor> vendorMap = vendorCodes.isEmpty()
                ? Collections.emptyMap()
                : financeVendorMapper.selectList(
                Wrappers.<FinanceVendor>lambdaQuery()
                        .in(FinanceVendor::getCVenCode, vendorCodes)
        ).stream().collect(Collectors.toMap(
                FinanceVendor::getCVenCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        List<Long> departmentIdValues = departmentIds.stream().map(this::toLong).filter(Objects::nonNull).toList();
        Map<String, String> departmentNameMap = departmentIdValues.isEmpty()
                ? Collections.emptyMap()
                : systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .in(SystemDepartment::getId, departmentIdValues)
        ).stream().collect(Collectors.toMap(
                item -> String.valueOf(item.getId()),
                SystemDepartment::getDeptName,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        Map<String, Map<String, String>> archiveItemLabelMap = loadArchiveItemLabelMap(archiveCodes);

        Map<String, SummaryMetadata> metadataMap = new LinkedHashMap<>();
        for (ProcessDocumentInstance instance : instances) {
            SummaryDraft draft = draftMap.get(instance.getDocumentCode());
            User submitter = instance.getSubmitterUserId() == null ? null : userMap.get(instance.getSubmitterUserId());
            SummaryMetadata metadata = new SummaryMetadata(
                    submitter == null || submitter.getDeptId() == null ? null : departmentNameMap.get(String.valueOf(submitter.getDeptId())),
                    draft == null ? null : draft.getPaymentCompanyId(),
                    draft == null ? null : resolvePaymentCompanyName(draft.getPaymentCompanyId(), companyMap),
                    draft == null ? null : resolvePartyName(draft.getPayeeValue(), userMap, vendorMap),
                    draft == null ? null : resolveVendorName(draft.getCounterpartyValue(), vendorMap),
                    draft == null ? null : draft.getPaymentDate(),
                    draft == null ? Collections.emptyList() : resolveDepartmentNames(draft.getUndertakeDepartmentIds(), departmentNameMap),
                    draft == null ? Collections.emptyList() : resolveArchiveItemNames(draft.getTagArchiveCode(), draft.getTagValues(), archiveItemLabelMap)
            );
            metadataMap.put(instance.getDocumentCode(), metadata);
        }
        return new SummaryEnrichmentData(metadataMap);
    }

    private Map<String, String> loadTagArchiveCodeByTemplateCode(Map<String, ProcessDocumentTemplate> templateMap) {
        if (templateMap.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> templateIds = templateMap.values().stream()
                .map(ProcessDocumentTemplate::getId)
                .filter(Objects::nonNull)
                .toList();
        if (templateIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> templateCodeById = templateMap.values().stream()
                .filter(item -> item.getId() != null && trimToNull(item.getTemplateCode()) != null)
                .collect(Collectors.toMap(
                        ProcessDocumentTemplate::getId,
                        ProcessDocumentTemplate::getTemplateCode,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        return processTemplateScopeMapper.selectList(
                Wrappers.<ProcessTemplateScope>lambdaQuery()
                        .in(ProcessTemplateScope::getTemplateId, templateIds)
                        .eq(ProcessTemplateScope::getOptionType, TEMPLATE_SCOPE_TYPE_TAG_ARCHIVE)
                        .orderByAsc(ProcessTemplateScope::getSortOrder, ProcessTemplateScope::getId)
        ).stream()
                .filter(item -> trimToNull(templateCodeById.get(item.getTemplateId())) != null)
                .collect(Collectors.toMap(
                        item -> templateCodeById.get(item.getTemplateId()),
                        ProcessTemplateScope::getOptionCode,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Map<String, Map<String, String>> loadArchiveItemLabelMap(Set<String> archiveCodes) {
        if (archiveCodes == null || archiveCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProcessCustomArchiveDesign> archives = customArchiveDesignMapper.selectList(
                Wrappers.<ProcessCustomArchiveDesign>lambdaQuery()
                        .in(ProcessCustomArchiveDesign::getArchiveCode, archiveCodes)
        );
        if (archives.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> archiveCodeById = archives.stream().collect(Collectors.toMap(
                ProcessCustomArchiveDesign::getId,
                ProcessCustomArchiveDesign::getArchiveCode,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        List<ProcessCustomArchiveItem> items = customArchiveItemMapper.selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .in(ProcessCustomArchiveItem::getArchiveId, archiveCodeById.keySet())
                        .eq(ProcessCustomArchiveItem::getStatus, 1)
                        .orderByAsc(ProcessCustomArchiveItem::getPriority, ProcessCustomArchiveItem::getId)
        );
        Map<String, Map<String, String>> labelMap = new LinkedHashMap<>();
        for (ProcessCustomArchiveItem item : items) {
            String archiveCode = archiveCodeById.get(item.getArchiveId());
            if (archiveCode == null) {
                continue;
            }
            labelMap.computeIfAbsent(archiveCode, ignored -> new LinkedHashMap<>())
                    .put(trimToNull(item.getItemCode()) == null ? item.getItemName() : item.getItemCode(), item.getItemName());
        }
        return labelMap;
    }

    private String extractFirstBusinessComponentValue(Map<String, Object> schema, Map<String, Object> formData, String componentCode) {
        if (schema == null || formData == null || trimToNull(componentCode) == null) {
            return null;
        }
        Object rawBlocks = schema.get("blocks");
        if (!(rawBlocks instanceof List<?> blocks)) {
            return null;
        }
        for (Object rawBlock : blocks) {
            if (!(rawBlock instanceof Map<?, ?> blockMap)) {
                continue;
            }
            if (!Objects.equals(String.valueOf(blockMap.get("kind")), "BUSINESS_COMPONENT")) {
                continue;
            }
            Object rawProps = blockMap.get("props");
            if (!(rawProps instanceof Map<?, ?> props)) {
                continue;
            }
            if (!Objects.equals(String.valueOf(props.get("componentCode")), componentCode)) {
                continue;
            }
            String fieldKey = trimToNull(String.valueOf(blockMap.get("fieldKey")));
            if (fieldKey == null) {
                continue;
            }
            String value = firstLookupValue(formData.get(fieldKey));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String extractPaymentDate(Map<String, Object> schema, Map<String, Object> formData) {
        if (schema == null || formData == null) {
            return null;
        }
        Object rawBlocks = schema.get("blocks");
        if (!(rawBlocks instanceof List<?> blocks)) {
            return null;
        }
        List<String> dateFieldKeys = new ArrayList<>();
        for (Object rawBlock : blocks) {
            if (!(rawBlock instanceof Map<?, ?> blockMap)) {
                continue;
            }
            Object rawProps = blockMap.get("props");
            if (!(rawProps instanceof Map<?, ?> props)) {
                continue;
            }
            if (!Objects.equals(CONTROL_TYPE_DATE, String.valueOf(props.get("controlType")))) {
                continue;
            }
            String fieldKey = trimObjectToNull(blockMap.get("fieldKey"));
            if (fieldKey == null) {
                continue;
            }
            dateFieldKeys.add(fieldKey);
            String label = trimObjectToNull(blockMap.get("label"));
            if (!isPaymentDateLabel(label) && !looksLikePaymentDateField(fieldKey)) {
                continue;
            }
            String value = firstStringValue(formData.get(fieldKey));
            if (value != null) {
                return value;
            }
        }
        if (dateFieldKeys.size() != 1) {
            return null;
        }
        return firstStringValue(formData.get(dateFieldKeys.get(0)));
    }

    private boolean isPaymentDateLabel(String label) {
        String normalized = trimToNull(label);
        return normalized != null && (PAYMENT_DATE_LABELS.contains(normalized) || PAYMENT_DATE_LEGACY_LABEL_ALIASES.contains(normalized));
    }

    private boolean looksLikePaymentDateField(String fieldKey) {
        String normalized = trimToNull(fieldKey);
        if (normalized == null) {
            return false;
        }
        String lowerCaseKey = normalized.toLowerCase();
        return lowerCaseKey.contains("payment") && lowerCaseKey.contains("date");
    }

    private static Set<String> buildLegacyUtf8AsGbkLabels(Set<String> labels) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        for (String label : labels) {
            aliases.add(new String(label.getBytes(StandardCharsets.UTF_8), LEGACY_GARBLED_CHARSET));
        }
        return Collections.unmodifiableSet(aliases);
    }

    private List<String> extractArchiveValues(Map<String, Object> schema, Map<String, Object> formData, String archiveCode) {
        if (schema == null || formData == null || trimToNull(archiveCode) == null) {
            return Collections.emptyList();
        }
        Object rawBlocks = schema.get("blocks");
        if (!(rawBlocks instanceof List<?> blocks)) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (Object rawBlock : blocks) {
            if (!(rawBlock instanceof Map<?, ?> blockMap)) {
                continue;
            }
            Object rawProps = blockMap.get("props");
            if (!(rawProps instanceof Map<?, ?> props)) {
                continue;
            }
            if (!Objects.equals(archiveCode, trimToNull(String.valueOf(props.get("archiveCode"))))) {
                continue;
            }
            String fieldKey = trimToNull(String.valueOf(blockMap.get("fieldKey")));
            if (fieldKey == null) {
                continue;
            }
            collectStringValues(values, formData.get(fieldKey));
        }
        return new ArrayList<>(values);
    }

    private void collectPartyLookupIds(String value, Set<Long> userIds, Set<String> vendorCodes) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return;
        }
        if (normalized.startsWith(PERSONAL_PAYEE_VALUE_PREFIX)) {
            return;
        }
        if (normalized.startsWith("USER:")) {
            Long userId = toLong(normalized.substring("USER:".length()));
            if (userId != null) {
                userIds.add(userId);
            }
            return;
        }
        collectVendorCode(normalized, vendorCodes);
    }

    private void collectVendorCode(String value, Set<String> vendorCodes) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return;
        }
        if (normalized.startsWith("VENDOR:")) {
            normalized = trimToNull(normalized.substring("VENDOR:".length()));
        }
        if (normalized != null) {
            vendorCodes.add(normalized);
        }
    }

    private String resolvePaymentCompanyName(String companyId, Map<String, SystemCompany> companyMap) {
        String normalized = trimToNull(companyId);
        if (normalized == null) {
            return null;
        }
        SystemCompany company = companyMap.get(normalized);
        return company == null ? normalized : firstNonBlank(company.getCompanyName(), company.getCompanyCode(), normalized);
    }

    private String resolvePartyName(String value, Map<Long, User> userMap, Map<String, FinanceVendor> vendorMap) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith(PERSONAL_PAYEE_VALUE_PREFIX)) {
            return trimToNull(normalized.substring(PERSONAL_PAYEE_VALUE_PREFIX.length()));
        }
        if (normalized.startsWith("USER:")) {
            Long userId = toLong(normalized.substring("USER:".length()));
            User user = userId == null ? null : userMap.get(userId);
            return user == null ? normalized : firstNonBlank(user.getName(), user.getUsername(), normalized);
        }
        return resolveVendorName(normalized, vendorMap);
    }

    private String resolveVendorName(String value, Map<String, FinanceVendor> vendorMap) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("VENDOR:")) {
            normalized = trimToNull(normalized.substring("VENDOR:".length()));
        }
        if (normalized == null) {
            return null;
        }
        FinanceVendor vendor = vendorMap.get(normalized);
        return vendor == null ? normalized : firstNonBlank(vendor.getCVenName(), vendor.getCVenAbbName(), normalized);
    }

    private List<String> resolveDepartmentNames(List<String> departmentIds, Map<String, String> departmentNameMap) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String departmentId : departmentIds) {
            String normalized = trimToNull(departmentId);
            if (normalized == null) {
                continue;
            }
            names.add(defaultText(trimToNull(departmentNameMap.get(normalized)), normalized));
        }
        return new ArrayList<>(names);
    }

    private List<String> resolveArchiveItemNames(
            String archiveCode,
            List<String> values,
            Map<String, Map<String, String>> archiveItemLabelMap
    ) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, String> labelMap = trimToNull(archiveCode) == null
                ? Collections.emptyMap()
                : archiveItemLabelMap.getOrDefault(archiveCode, Collections.emptyMap());
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized == null) {
                continue;
            }
            names.add(defaultText(trimToNull(labelMap.get(normalized)), normalized));
        }
        return new ArrayList<>(names);
    }

    private void collectStringValues(Set<String> result, Object value) {
        if (value instanceof List<?> items) {
            for (Object item : items) {
                String normalized = firstLookupValue(item);
                if (normalized != null) {
                    result.add(normalized);
                }
            }
            return;
        }
        String normalized = firstLookupValue(value);
        if (normalized != null) {
            result.add(normalized);
        }
    }

    private String firstStringValue(Object value) {
        if (value instanceof List<?> items) {
            for (Object item : items) {
                String normalized = trimToNull(item == null ? null : String.valueOf(item));
                if (normalized != null) {
                    return normalized;
                }
            }
            return null;
        }
        return trimToNull(value == null ? null : String.valueOf(value));
    }

    private String firstLookupValue(Object value) {
        if (value instanceof List<?> items) {
            for (Object item : items) {
                String normalized = extractLookupValue(item);
                if (normalized != null) {
                    return normalized;
                }
            }
            return null;
        }
        return extractLookupValue(value);
    }

    private String extractLookupValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            String normalized = firstNonBlank(
                    trimObjectToNull(map.get("value")),
                    trimObjectToNull(map.get("code")),
                    trimObjectToNull(map.get("id")),
                    trimObjectToNull(map.get("sourceCode"))
            );
            if (normalized != null) {
                return normalized;
            }
            return trimObjectToNull(map.get("label"));
        }
        return trimToNull(value == null ? null : String.valueOf(value));
    }

    private String normalizePayeeName(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith(PERSONAL_PAYEE_VALUE_PREFIX)) {
            return trimToNull(normalized.substring(PERSONAL_PAYEE_VALUE_PREFIX.length()));
        }
        return normalized;
    }

    private String trimObjectToNull(Object value) {
        return trimToNull(value == null ? null : String.valueOf(value));
    }

    private Long toLong(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Long.valueOf(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<String> resolveUndertakeDeptIdsFromSnapshots(
            Map<String, Object> mainSchema,
            Map<String, Object> mainFormData,
            List<ProcessDocumentExpenseDetail> expenseDetails
    ) {
        Set<String> deptIds = new LinkedHashSet<>();
        collectUndertakeDeptIdsFromSchema(deptIds, mainSchema, mainFormData);
        if (expenseDetails != null) {
            for (ProcessDocumentExpenseDetail expenseDetail : expenseDetails) {
                collectUndertakeDeptIdsFromSchema(
                        deptIds,
                        readMap(expenseDetail.getSchemaSnapshotJson()),
                        readMap(expenseDetail.getFormDataJson())
                );
            }
        }
        return new ArrayList<>(deptIds);
    }

    private void collectUndertakeDeptIdsFromSchema(Set<String> result, Map<String, Object> schema, Map<String, Object> formData) {
        if (schema == null || formData == null || formData.isEmpty()) {
            return;
        }
        Object rawBlocks = schema.get("blocks");
        if (!(rawBlocks instanceof List<?> blocks)) {
            return;
        }
        for (Object rawBlock : blocks) {
            if (!(rawBlock instanceof Map<?, ?> blockMap)) {
                continue;
            }
            if (!Objects.equals(String.valueOf(blockMap.get("kind")), "BUSINESS_COMPONENT")) {
                continue;
            }
            Object rawProps = blockMap.get("props");
            if (!(rawProps instanceof Map<?, ?> props)) {
                continue;
            }
            if (!Objects.equals(String.valueOf(props.get("componentCode")), UNDERTAKE_DEPARTMENT_COMPONENT_CODE)) {
                continue;
            }
            String fieldKey = trimToNull(String.valueOf(blockMap.get("fieldKey")));
            if (fieldKey != null) {
                collectDeptIds(result, formData.get(fieldKey));
            }
        }
    }

    private void collectDeptIds(Set<String> result, Object value) {
        if (value instanceof List<?> items) {
            for (Object item : items) {
                String normalized = trimToNull(String.valueOf(item));
                if (normalized != null) {
                    result.add(normalized);
                }
            }
            return;
        }
        String normalized = trimToNull(value == null ? null : String.valueOf(value));
        if (normalized != null) {
            result.add(normalized);
        }
    }

    private Map<String, Object> readSchema(String schemaJson) {
        if (trimToNull(schemaJson) == null) {
            return defaultSchema();
        }
        try {
            return objectMapper.readValue(schemaJson, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse form schema", ex);
        }
    }

    private Map<String, Object> defaultSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("layoutMode", "TWO_COLUMN");
        schema.put("blocks", Collections.emptyList());
        return schema;
    }

    private Map<String, Object> readMap(String json) {
        if (trimToNull(json) == null) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse json map", ex);
        }
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String resolveTemplateTypeLabel(String templateType, String currentLabel) {
        if (trimToNull(currentLabel) != null) {
            return currentLabel;
        }
        return switch (trimToNull(templateType) == null ? "report" : templateType.trim()) {
            case "application" -> "\u7533\u8bf7\u5355";
            case "loan" -> "\u501f\u6b3e\u5355";
            case "contract" -> "\u5408\u540c\u5355";
            default -> "\u62a5\u9500\u5355";
        };
    }

    private String resolveStatusLabel(String status) {
        return switch (trimToNull(status) == null ? "" : status.trim()) {
            case DOCUMENT_STATUS_PENDING_PAYMENT -> "\u5f85\u652f\u4ed8";
            case DOCUMENT_STATUS_PAYING -> "\u652f\u4ed8\u4e2d";
            case DOCUMENT_STATUS_PAYMENT_COMPLETED -> "\u5df2\u652f\u4ed8";
            case DOCUMENT_STATUS_PAYMENT_FINISHED -> "\u5df2\u5b8c\u6210";
            case DOCUMENT_STATUS_PAYMENT_EXCEPTION -> "\u652f\u4ed8\u5f02\u5e38";
            case DOCUMENT_STATUS_APPROVED -> "\u5df2\u901a\u8fc7";
            case DOCUMENT_STATUS_REJECTED -> "\u5df2\u9a73\u56de";
            case "DRAFT" -> "\u8349\u7a3f";
            case DOCUMENT_STATUS_EXCEPTION -> "\u6d41\u7a0b\u5f02\u5e38";
            default -> "\u5ba1\u6279\u4e2d";
        };
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String defaultReason(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "\u6682\u65e0\u4e8b\u7531" : normalized;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class SummaryEnrichmentData {
        private static final SummaryEnrichmentData EMPTY = new SummaryEnrichmentData(Collections.emptyMap());
        private final Map<String, SummaryMetadata> metadataByDocumentCode;

        private SummaryEnrichmentData(Map<String, SummaryMetadata> metadataByDocumentCode) {
            this.metadataByDocumentCode = metadataByDocumentCode;
        }

        private static SummaryEnrichmentData empty() {
            return EMPTY;
        }

        private SummaryMetadata metadata(String documentCode) {
            return metadataByDocumentCode.getOrDefault(documentCode, SummaryMetadata.empty());
        }
    }

    private static final class SummaryMetadata {
        private static final SummaryMetadata EMPTY = new SummaryMetadata(
                null,
                null,
                null,
                null,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList()
        );

        private final String submitterDeptName;
        private final String paymentCompanyId;
        private final String paymentCompanyName;
        private final String payeeName;
        private final String counterpartyName;
        private final String paymentDate;
        private final List<String> undertakeDepartmentNames;
        private final List<String> tagNames;

        private SummaryMetadata(
                String submitterDeptName,
                String paymentCompanyId,
                String paymentCompanyName,
                String payeeName,
                String counterpartyName,
                String paymentDate,
                List<String> undertakeDepartmentNames,
                List<String> tagNames
        ) {
            this.submitterDeptName = submitterDeptName;
            this.paymentCompanyId = paymentCompanyId;
            this.paymentCompanyName = paymentCompanyName;
            this.payeeName = payeeName;
            this.counterpartyName = counterpartyName;
            this.paymentDate = paymentDate;
            this.undertakeDepartmentNames = undertakeDepartmentNames == null ? Collections.emptyList() : undertakeDepartmentNames;
            this.tagNames = tagNames == null ? Collections.emptyList() : tagNames;
        }

        private static SummaryMetadata empty() {
            return EMPTY;
        }

        private String submitterDeptName() {
            return submitterDeptName;
        }

        private String paymentCompanyId() {
            return paymentCompanyId;
        }

        private String paymentCompanyName() {
            return paymentCompanyName;
        }

        private String payeeName() {
            return payeeName;
        }

        private String counterpartyName() {
            return counterpartyName;
        }

        private String paymentDate() {
            return paymentDate;
        }

        private List<String> undertakeDepartmentNames() {
            return undertakeDepartmentNames;
        }

        private List<String> tagNames() {
            return tagNames;
        }
    }

    private static final class SummaryDraft {
        private String documentCode;
        private String paymentCompanyId;
        private String payeeValue;
        private String counterpartyValue;
        private String paymentDate;
        private List<String> undertakeDepartmentIds = Collections.emptyList();
        private String tagArchiveCode;
        private List<String> tagValues = Collections.emptyList();

        private void setDocumentCode(String documentCode) {
            this.documentCode = documentCode;
        }

        private void setPaymentCompanyId(String paymentCompanyId) {
            this.paymentCompanyId = paymentCompanyId;
        }

        private void setPayeeValue(String payeeValue) {
            this.payeeValue = payeeValue;
        }

        private void setCounterpartyValue(String counterpartyValue) {
            this.counterpartyValue = counterpartyValue;
        }

        private void setPaymentDate(String paymentDate) {
            this.paymentDate = paymentDate;
        }

        private void setUndertakeDepartmentIds(List<String> undertakeDepartmentIds) {
            this.undertakeDepartmentIds = undertakeDepartmentIds == null ? Collections.emptyList() : undertakeDepartmentIds;
        }

        private void setTagArchiveCode(String tagArchiveCode) {
            this.tagArchiveCode = tagArchiveCode;
        }

        private void setTagValues(List<String> tagValues) {
            this.tagValues = tagValues == null ? Collections.emptyList() : tagValues;
        }

        private String getPaymentCompanyId() {
            return paymentCompanyId;
        }

        private String getPayeeValue() {
            return payeeValue;
        }

        private String getCounterpartyValue() {
            return counterpartyValue;
        }

        private String getPaymentDate() {
            return paymentDate;
        }

        private List<String> getUndertakeDepartmentIds() {
            return undertakeDepartmentIds;
        }

        private String getTagArchiveCode() {
            return tagArchiveCode;
        }

        private List<String> getTagValues() {
            return tagValues;
        }

    }

}
