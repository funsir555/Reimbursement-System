package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseDocumentPickerGroupVO;
import com.finex.auth.dto.ExpenseDocumentPickerItemVO;
import com.finex.auth.dto.ExpenseDocumentRelationBindingVO;
import com.finex.auth.dto.ExpenseDocumentWriteOffBindingVO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentRelation;
import com.finex.auth.entity.ProcessDocumentWriteOff;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentRelationMapper;
import com.finex.auth.mapper.ProcessDocumentWriteOffMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

abstract class AbstractExpenseRelationWriteOffSupport {

    protected static final String RELATED_DOCUMENT_COMPONENT_CODE = "related-document";
    protected static final String WRITEOFF_DOCUMENT_COMPONENT_CODE = "writeoff-document";
    protected static final String RELATION_TYPE_RELATED = "RELATED";
    protected static final String RELATION_TYPE_WRITEOFF = "WRITEOFF";
    protected static final String RELATION_STATUS_ACTIVE = "ACTIVE";
    protected static final String RELATION_STATUS_VOID = "VOID";
    protected static final String WRITEOFF_STATUS_PENDING = "PENDING_EFFECTIVE";
    protected static final String WRITEOFF_STATUS_EFFECTIVE = "EFFECTIVE";
    protected static final String WRITEOFF_STATUS_VOID = "VOID";
    protected static final String BINDING_DIRECTION_OUTBOUND = "OUTBOUND";
    protected static final String BINDING_DIRECTION_INBOUND = "INBOUND";
    protected static final String WRITEOFF_SOURCE_LOAN = "LOAN";
    protected static final String WRITEOFF_SOURCE_PREPAY_REPORT = "PREPAY_REPORT";
    protected static final String DASHBOARD_WRITEOFF_SOURCE_FIELD_KEY = "dashboard-writeoff";
    protected static final String ENTERPRISE_MODE_PREPAY_UNBILLED = "PREPAY_UNBILLED";
    protected static final String DOCUMENT_STATUS_APPROVED = "APPROVED";
    protected static final String DOCUMENT_STATUS_COMPLETED = "COMPLETED";
    protected static final String DOCUMENT_STATUS_REJECTED = "REJECTED";
    protected static final String DOCUMENT_STATUS_EXCEPTION = "EXCEPTION";
    protected static final String DOCUMENT_STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    protected static final String DOCUMENT_STATUS_PAYING = "PAYING";
    protected static final String DOCUMENT_STATUS_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    protected static final String DOCUMENT_STATUS_PAYMENT_FINISHED = "PAYMENT_FINISHED";
    protected static final String DOCUMENT_STATUS_PAYMENT_EXCEPTION = "PAYMENT_EXCEPTION";
    protected static final String MESSAGE_RELATED_TEMPLATE_TYPE_NOT_ALLOWED = "\u5173\u8054\u5355\u636e\u7c7b\u578b\u4e0d\u5728\u5f53\u524d\u7ec4\u4ef6\u5141\u8bb8\u8303\u56f4\u5185";
    protected static final String MESSAGE_WRITEOFF_TEMPLATE_TYPE_NOT_ALLOWED = "\u6838\u9500\u5355\u636e\u7c7b\u578b\u4e0d\u5728\u5f53\u524d\u7ec4\u4ef6\u5141\u8bb8\u8303\u56f4\u5185";
    protected static final String MESSAGE_RELATED_DOCUMENT_SCOPE_RESTRICTED = "\u4ec5\u53ef\u5173\u8054\u672c\u4eba\u5f85\u652f\u4ed8\u3001\u652f\u4ed8\u4e2d\u3001\u5df2\u652f\u4ed8\u6216\u5df2\u5b8c\u6210\u7684\u5355\u636e";
    protected static final String MESSAGE_WRITEOFF_DOCUMENT_SCOPE_RESTRICTED = "\u4ec5\u53ef\u9009\u62e9\u672c\u4eba\u5f85\u652f\u4ed8\u3001\u652f\u4ed8\u4e2d\u3001\u5df2\u652f\u4ed8\u6216\u5df2\u5b8c\u6210\u7684\u5355\u636e\u8fdb\u884c\u6838\u9500";
    protected static final List<String> RELATION_PICKER_ALLOWED_STATUSES = List.of(
            DOCUMENT_STATUS_PENDING_PAYMENT,
            DOCUMENT_STATUS_PAYING,
            DOCUMENT_STATUS_PAYMENT_COMPLETED,
            DOCUMENT_STATUS_PAYMENT_FINISHED
    );

    protected final ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    protected final ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    protected final ProcessDocumentRelationMapper processDocumentRelationMapper;
    protected final ProcessDocumentWriteOffMapper processDocumentWriteOffMapper;
    protected final ObjectMapper objectMapper;

    protected AbstractExpenseRelationWriteOffSupport(ExpenseRelationWriteOffSupportContext context) {
        this.processDocumentInstanceMapper = context.processDocumentInstanceMapper;
        this.processDocumentExpenseDetailMapper = context.processDocumentExpenseDetailMapper;
        this.processDocumentRelationMapper = context.processDocumentRelationMapper;
        this.processDocumentWriteOffMapper = context.processDocumentWriteOffMapper;
        this.objectMapper = context.objectMapper;
    }

    protected String normalizeDashboardOutstandingKind(String kind) {
        String normalizedKind = trimToNull(kind);
        if (Objects.equals(normalizedKind, WRITEOFF_SOURCE_LOAN) || Objects.equals(normalizedKind, WRITEOFF_SOURCE_PREPAY_REPORT)) {
            return normalizedKind;
        }
        throw new IllegalArgumentException("涓嶆敮鎸佺殑寰呭鐞嗗崟鎹被鍨?");
    }

    protected String normalizeRelationType(String relationType) {
        return Objects.equals(trimToNull(relationType), RELATION_TYPE_WRITEOFF) ? RELATION_TYPE_WRITEOFF : RELATION_TYPE_RELATED;
    }

    protected List<String> normalizePickerTemplateTypes(String relationType, List<String> templateTypes) {
        if (Objects.equals(relationType, RELATION_TYPE_WRITEOFF)) {
            if (templateTypes == null || templateTypes.isEmpty()) {
                return List.of("report", "loan");
            }
            return templateTypes.stream()
                    .map(this::normalizeTemplateType)
                    .filter(item -> Objects.equals(item, "report") || Objects.equals(item, "loan"))
                    .distinct()
                    .toList();
        }
        if (templateTypes == null || templateTypes.isEmpty()) {
            return List.of("report", "application", "contract", "loan");
        }
        return templateTypes.stream()
                .map(this::normalizeTemplateType)
                .distinct()
                .toList();
    }

    protected String normalizeTemplateType(String templateType) {
        String value = trimToNull(templateType);
        if (Objects.equals(value, "application") || Objects.equals(value, "loan") || Objects.equals(value, "contract")) {
            return value;
        }
        return "report";
    }

    protected List<String> normalizeAllowedTemplateTypes(String componentCode, Object rawValue) {
        boolean writeOffComponent = Objects.equals(componentCode, WRITEOFF_DOCUMENT_COMPONENT_CODE);
        if (!(rawValue instanceof List<?> values) || values.isEmpty()) {
            return writeOffComponent ? List.of("report", "loan") : List.of("report", "application", "contract", "loan");
        }
        List<String> normalized = values.stream()
                .map(item -> normalizeTemplateType(item == null ? null : String.valueOf(item)))
                .filter(item -> !writeOffComponent || Objects.equals(item, "report") || Objects.equals(item, "loan"))
                .distinct()
                .toList();
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return writeOffComponent ? List.of("report", "loan") : List.of("report", "application", "contract", "loan");
    }

    protected boolean isEffectiveApprovedStatus(String status) {
        String normalized = trimToNull(status);
        return DOCUMENT_STATUS_APPROVED.equals(normalized)
                || DOCUMENT_STATUS_COMPLETED.equals(normalized)
                || DOCUMENT_STATUS_PENDING_PAYMENT.equals(normalized)
                || DOCUMENT_STATUS_PAYMENT_COMPLETED.equals(normalized)
                || DOCUMENT_STATUS_PAYMENT_FINISHED.equals(normalized);
    }

    protected boolean isRelationSelectableStatus(String status) {
        String normalized = trimToNull(status);
        return DOCUMENT_STATUS_PENDING_PAYMENT.equals(normalized)
                || DOCUMENT_STATUS_PAYING.equals(normalized)
                || DOCUMENT_STATUS_PAYMENT_COMPLETED.equals(normalized)
                || DOCUMENT_STATUS_PAYMENT_FINISHED.equals(normalized);
    }

    protected boolean matchesKeyword(String keyword, String... values) {
        if (keyword == null) {
            return true;
        }
        for (String value : values) {
            if (value != null && value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    protected String resolveTemplateTypeLabel(String templateType, String currentLabel) {
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

    protected String resolveStatusLabel(String status) {
        return switch (trimToNull(status) == null ? "" : status.trim()) {
            case DOCUMENT_STATUS_PENDING_PAYMENT -> "\u5f85\u652f\u4ed8";
            case DOCUMENT_STATUS_PAYING -> "\u652f\u4ed8\u4e2d";
            case DOCUMENT_STATUS_PAYMENT_COMPLETED -> "\u5df2\u652f\u4ed8";
            case DOCUMENT_STATUS_PAYMENT_FINISHED -> "\u5df2\u5b8c\u6210";
            case DOCUMENT_STATUS_PAYMENT_EXCEPTION -> "\u652f\u4ed8\u5f02\u5e38";
            case DOCUMENT_STATUS_APPROVED, DOCUMENT_STATUS_COMPLETED -> "\u5df2\u5b8c\u6210";
            case DOCUMENT_STATUS_REJECTED -> "\u5df2\u9a73\u56de";
            case "DRAFT" -> "\u8349\u7a3f";
            case DOCUMENT_STATUS_EXCEPTION -> "\u6d41\u7a0b\u5f02\u5e38";
            default -> "\u5ba1\u6279\u4e2d";
        };
    }

    protected String resolveWriteOffStatusLabel(String status) {
        return switch (trimToNull(status) == null ? "" : status.trim()) {
            case WRITEOFF_STATUS_PENDING -> "\u5f85\u751f\u6548";
            case WRITEOFF_STATUS_EFFECTIVE -> "\u5df2\u751f\u6548";
            case WRITEOFF_STATUS_VOID -> "\u5df2\u4f5c\u5e9f";
            default -> "-";
        };
    }

    protected ProcessDocumentInstance requireDocument(String documentCode) {
        String normalizedCode = trimToNull(documentCode);
        if (normalizedCode == null) {
            throw new IllegalArgumentException("Document code is required");
        }
        ProcessDocumentInstance instance = processDocumentInstanceMapper.selectOne(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getDocumentCode, normalizedCode)
                        .last("limit 1")
        );
        if (instance == null) {
            throw new IllegalStateException("Document not found");
        }
        return instance;
    }

    protected void requireSubmitter(ProcessDocumentInstance instance, Long userId) {
        if (!Objects.equals(instance.getSubmitterUserId(), userId)) {
            throw new IllegalStateException("鍙湁鎻愬崟浜哄彲浠ユ墽琛屽綋鍓嶆搷浣?");
        }
    }

    protected ProcessDocumentInstance requireRelationSelectableTargetDocument(
            Map<String, ProcessDocumentInstance> targetDocumentMap,
            String documentCode,
            Long submitterUserId,
            String invalidMessage
    ) {
        ProcessDocumentInstance target = targetDocumentMap.get(documentCode);
        if (target == null
                || !Objects.equals(target.getSubmitterUserId(), submitterUserId)
                || !isRelationSelectableStatus(target.getStatus())) {
            throw new IllegalStateException(invalidMessage);
        }
        return target;
    }

    protected ProcessDocumentInstance requireApprovedTargetDocument(
            Map<String, ProcessDocumentInstance> targetDocumentMap,
            String documentCode,
            String actionName
    ) {
        ProcessDocumentInstance target = targetDocumentMap.get(documentCode);
        if (target == null || !isEffectiveApprovedStatus(target.getStatus())) {
            throw new IllegalStateException(actionName + "鐩爣涓嶅瓨鍦ㄦ垨鏈€氳繃瀹℃壒");
        }
        return target;
    }

    protected Map<String, ProcessDocumentInstance> loadDocumentMap(Set<String> documentCodes) {
        if (documentCodes == null || documentCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .in(ProcessDocumentInstance::getDocumentCode, documentCodes)
        ).stream().collect(Collectors.toMap(
                ProcessDocumentInstance::getDocumentCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    protected ExpenseDocumentRelationBindingVO toRelationBinding(
            ProcessDocumentRelation relation,
            String direction,
            ProcessDocumentInstance document,
            String fallbackTemplateType
    ) {
        ExpenseDocumentRelationBindingVO binding = new ExpenseDocumentRelationBindingVO();
        String documentCode = Objects.equals(direction, BINDING_DIRECTION_OUTBOUND)
                ? relation.getTargetDocumentCode()
                : relation.getSourceDocumentCode();
        String templateType = normalizeTemplateType(document == null ? fallbackTemplateType : document.getTemplateType());
        binding.setDirection(direction);
        binding.setFieldKey(relation.getSourceFieldKey());
        binding.setDocumentCode(documentCode);
        binding.setDocumentTitle(document == null ? documentCode : document.getDocumentTitle());
        binding.setTemplateType(templateType);
        binding.setTemplateTypeLabel(resolveTemplateTypeLabel(templateType, null));
        binding.setTemplateName(document == null ? null : document.getTemplateName());
        binding.setStatus(document == null ? null : document.getStatus());
        binding.setStatusLabel(document == null ? null : resolveStatusLabel(document.getStatus()));
        binding.setSubmitterName(document == null ? null : document.getSubmitterName());
        return binding;
    }

    protected ExpenseDocumentWriteOffBindingVO toWriteOffBinding(
            ProcessDocumentWriteOff writeOff,
            String direction,
            ProcessDocumentInstance document,
            String fallbackTemplateType
    ) {
        ExpenseDocumentWriteOffBindingVO binding = new ExpenseDocumentWriteOffBindingVO();
        String documentCode = Objects.equals(direction, BINDING_DIRECTION_OUTBOUND)
                ? writeOff.getTargetDocumentCode()
                : writeOff.getSourceDocumentCode();
        String templateType = normalizeTemplateType(document == null ? fallbackTemplateType : document.getTemplateType());
        binding.setDirection(direction);
        binding.setFieldKey(writeOff.getSourceFieldKey());
        binding.setDocumentCode(documentCode);
        binding.setDocumentTitle(document == null ? documentCode : document.getDocumentTitle());
        binding.setTemplateType(templateType);
        binding.setTemplateTypeLabel(resolveTemplateTypeLabel(templateType, null));
        binding.setTemplateName(document == null ? null : document.getTemplateName());
        binding.setStatus(document == null ? null : document.getStatus());
        binding.setStatusLabel(document == null ? null : resolveStatusLabel(document.getStatus()));
        binding.setSubmitterName(document == null ? null : document.getSubmitterName());
        binding.setWriteOffSourceKind(writeOff.getWriteoffSourceKind());
        binding.setRequestedAmount(defaultDecimal(writeOff.getRequestedAmount()));
        binding.setEffectiveAmount(defaultDecimal(writeOff.getEffectiveAmount()));
        binding.setRemainingAmount(defaultDecimal(writeOff.getRemainingSnapshotAmount()));
        binding.setEffectiveStatus(writeOff.getStatus());
        binding.setEffectiveStatusLabel(resolveWriteOffStatusLabel(writeOff.getStatus()));
        return binding;
    }

    protected ExpenseDocumentPickerGroupVO paginatePickerGroup(
            String templateType,
            List<ExpenseDocumentPickerItemVO> items,
            int page,
            int pageSize
    ) {
        ExpenseDocumentPickerGroupVO group = new ExpenseDocumentPickerGroupVO();
        group.setTemplateType(templateType);
        group.setTemplateTypeLabel(resolveTemplateTypeLabel(templateType, null));
        group.setPage(page);
        group.setPageSize(pageSize);
        group.setTotal(items.size());
        int fromIndex = Math.min(Math.max((page - 1) * pageSize, 0), items.size());
        int toIndex = Math.min(fromIndex + pageSize, items.size());
        group.setItems(new ArrayList<>(items.subList(fromIndex, toIndex)));
        return group;
    }

    protected ExpenseDocumentPickerItemVO toPickerItem(ProcessDocumentInstance instance) {
        ExpenseDocumentPickerItemVO item = new ExpenseDocumentPickerItemVO();
        item.setDocumentCode(instance.getDocumentCode());
        item.setDocumentTitle(instance.getDocumentTitle());
        item.setTemplateType(instance.getTemplateType());
        item.setTemplateTypeLabel(resolveTemplateTypeLabel(instance.getTemplateType(), null));
        item.setTemplateName(instance.getTemplateName());
        item.setStatus(instance.getStatus());
        item.setStatusLabel(resolveStatusLabel(instance.getStatus()));
        item.setTotalAmount(defaultDecimal(instance.getTotalAmount()));
        return item;
    }

    protected List<DocumentBusinessBinding> collectDocumentBusinessBindings(ProcessFormDesign formDesign) {
        Map<String, Object> schema = readSchema(formDesign.getSchemaJson());
        Object rawBlocks = schema.get("blocks");
        if (!(rawBlocks instanceof List<?> blocks) || blocks.isEmpty()) {
            return Collections.emptyList();
        }
        List<DocumentBusinessBinding> bindings = new ArrayList<>();
        for (Object rawBlock : blocks) {
            if (!(rawBlock instanceof Map<?, ?> blockMap)) {
                continue;
            }
            if (!Objects.equals(String.valueOf(blockMap.get("kind")), "BUSINESS_COMPONENT")) {
                continue;
            }
            Object rawFieldKey = blockMap.get("fieldKey");
            Object rawProps = blockMap.get("props");
            if (!(rawProps instanceof Map<?, ?> propsMap) || rawFieldKey == null) {
                continue;
            }
            String componentCode = asText(propsMap.get("componentCode"));
            String fieldKey = asText(rawFieldKey);
            if (fieldKey == null || componentCode == null) {
                continue;
            }
            if (!Objects.equals(componentCode, RELATED_DOCUMENT_COMPONENT_CODE)
                    && !Objects.equals(componentCode, WRITEOFF_DOCUMENT_COMPONENT_CODE)) {
                continue;
            }
            bindings.add(new DocumentBusinessBinding(fieldKey, componentCode, normalizeAllowedTemplateTypes(componentCode, propsMap.get("allowedTemplateTypes"))));
        }
        return bindings;
    }

    protected List<RelatedDocumentSelection> normalizeRelatedDocumentSelections(
            String documentCode,
            DocumentBusinessBinding binding,
            Map<String, Object> formData
    ) {
        Object rawValue = formData == null ? null : formData.get(binding.fieldKey());
        List<Map<String, Object>> records = normalizeDocumentRecords(rawValue);
        List<RelatedDocumentSelection> selections = new ArrayList<>();
        Set<String> seenCodes = new LinkedHashSet<>();
        int sortOrder = 1;
        for (Map<String, Object> record : records) {
            String targetDocumentCode = trimToNull(asText(record.get("documentCode")));
            if (targetDocumentCode == null || !seenCodes.add(targetDocumentCode)) {
                continue;
            }
            if (Objects.equals(targetDocumentCode, documentCode)) {
                throw new IllegalStateException("褰撳墠鍗曟嵁涓嶈兘鍏宠仈鑷繁");
            }
            selections.add(new RelatedDocumentSelection(binding.fieldKey(), targetDocumentCode, binding.allowedTemplateTypes(), sortOrder++));
        }
        return selections;
    }

    protected List<WriteOffSelection> normalizeWriteOffSelections(
            String documentCode,
            DocumentBusinessBinding binding,
            Map<String, Object> formData
    ) {
        Object rawValue = formData == null ? null : formData.get(binding.fieldKey());
        List<Map<String, Object>> records = normalizeDocumentRecords(rawValue);
        List<WriteOffSelection> selections = new ArrayList<>();
        Set<String> seenCodes = new LinkedHashSet<>();
        int sortOrder = 1;
        for (Map<String, Object> record : records) {
            String targetDocumentCode = trimToNull(asText(record.get("documentCode")));
            if (targetDocumentCode == null || !seenCodes.add(targetDocumentCode)) {
                continue;
            }
            if (Objects.equals(targetDocumentCode, documentCode)) {
                throw new IllegalStateException("褰撳墠鍗曟嵁涓嶈兘鏍搁攢鑷繁");
            }
            BigDecimal requestedAmount = toBigDecimal(record.get("writeOffAmount"));
            if (requestedAmount == null) {
                throw new IllegalStateException("鏍搁攢鍗曟嵁缂哄皯鏍搁攢閲戦");
            }
            selections.add(new WriteOffSelection(binding.fieldKey(), targetDocumentCode, binding.allowedTemplateTypes(), requestedAmount, sortOrder++));
        }
        return selections;
    }

    protected List<Map<String, Object>> normalizeDocumentRecords(Object rawValue) {
        if (rawValue == null) {
            return Collections.emptyList();
        }
        if (rawValue instanceof List<?> values) {
            List<Map<String, Object>> records = new ArrayList<>();
            for (Object value : values) {
                if (value instanceof Map<?, ?> map) {
                    records.add(toObjectMap(map));
                }
            }
            return records;
        }
        if (rawValue instanceof Map<?, ?> map) {
            return List.of(toObjectMap(map));
        }
        return Collections.emptyList();
    }

    protected Map<String, Object> toObjectMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value);
            }
        });
        return result;
    }

    protected Map<String, Object> readSchema(String schemaJson) {
        if (trimToNull(schemaJson) == null) {
            return defaultSchema();
        }
        try {
            return objectMapper.readValue(schemaJson, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse form schema", ex);
        }
    }

    protected Map<String, Object> defaultSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("layoutMode", "TWO_COLUMN");
        schema.put("blocks", Collections.emptyList());
        return schema;
    }

    protected BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    protected BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(String.valueOf(number));
        }
        try {
            String normalized = trimToNull(String.valueOf(value));
            return normalized == null ? null : new BigDecimal(normalized);
        } catch (Exception ex) {
            return null;
        }
    }

    protected String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    protected record DocumentBusinessBinding(
            String fieldKey,
            String componentCode,
            List<String> allowedTemplateTypes
    ) {
    }

    protected record RelatedDocumentSelection(
            String fieldKey,
            String documentCode,
            List<String> allowedTemplateTypes,
            int sortOrder
    ) {
    }

    protected record WriteOffSelection(
            String fieldKey,
            String documentCode,
            List<String> allowedTemplateTypes,
            BigDecimal requestedAmount,
            int sortOrder
    ) {
    }
}
