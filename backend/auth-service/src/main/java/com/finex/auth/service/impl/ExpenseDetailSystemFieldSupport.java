package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.entity.ProcessExpenseType;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ExpenseDetailSystemFieldSupport {

    public static final String DETAIL_TYPE_NORMAL = "NORMAL_REIMBURSEMENT";
    public static final String DETAIL_TYPE_ENTERPRISE = "ENTERPRISE_TRANSACTION";
    public static final String MODE_PREPAY_UNBILLED = "PREPAY_UNBILLED";
    public static final String MODE_INVOICE_FULL_PAYMENT = "INVOICE_FULL_PAYMENT";
    public static final String INVOICE_FREE_MODE_REQUIRED = "NOT_FREE";

    public static final String FIELD_EXPENSE_TYPE_CODE = "expenseTypeCode";
    public static final String FIELD_BUSINESS_SCENARIO = "businessScenario";
    public static final String FIELD_INVOICE_AMOUNT = "invoiceAmount";
    public static final String FIELD_ACTUAL_PAYMENT_AMOUNT = "actualPaymentAmount";
    public static final String FIELD_INVOICE_ATTACHMENTS = "invoiceAttachments";
    public static final String FIELD_PENDING_WRITE_OFF_AMOUNT = "pendingWriteOffAmount";

    public static final String SYSTEM_EXPENSE_TYPE = "EXPENSE_TYPE";
    public static final String SYSTEM_BUSINESS_SCENARIO = "BUSINESS_SCENARIO";
    public static final String SYSTEM_INVOICE_AMOUNT = "INVOICE_AMOUNT";
    public static final String SYSTEM_ACTUAL_PAYMENT_AMOUNT = "ACTUAL_PAYMENT_AMOUNT";
    public static final String SYSTEM_INVOICE_ATTACHMENTS = "INVOICE_ATTACHMENTS";
    public static final String SYSTEM_PENDING_WRITE_OFF_AMOUNT = "PENDING_WRITE_OFF_AMOUNT";

    private static final String CONTROL_TYPE_SELECT = "SELECT";
    private static final String CONTROL_TYPE_AMOUNT = "AMOUNT";
    private static final String CONTROL_TYPE_ATTACHMENT = "ATTACHMENT";
    private static final Map<String, String> SYSTEM_FIELD_KEYS = Map.of(
            SYSTEM_EXPENSE_TYPE, FIELD_EXPENSE_TYPE_CODE,
            SYSTEM_BUSINESS_SCENARIO, FIELD_BUSINESS_SCENARIO,
            SYSTEM_INVOICE_AMOUNT, FIELD_INVOICE_AMOUNT,
            SYSTEM_ACTUAL_PAYMENT_AMOUNT, FIELD_ACTUAL_PAYMENT_AMOUNT,
            SYSTEM_INVOICE_ATTACHMENTS, FIELD_INVOICE_ATTACHMENTS,
            SYSTEM_PENDING_WRITE_OFF_AMOUNT, FIELD_PENDING_WRITE_OFF_AMOUNT
    );
    private static final List<String> NORMAL_SYSTEM_FIELD_ORDER = List.of(
            SYSTEM_EXPENSE_TYPE,
            SYSTEM_BUSINESS_SCENARIO,
            SYSTEM_INVOICE_AMOUNT,
            SYSTEM_ACTUAL_PAYMENT_AMOUNT,
            SYSTEM_INVOICE_ATTACHMENTS
    );
    private static final List<String> ENTERPRISE_SYSTEM_FIELD_ORDER = List.of(
            SYSTEM_EXPENSE_TYPE,
            SYSTEM_BUSINESS_SCENARIO,
            SYSTEM_INVOICE_AMOUNT,
            SYSTEM_ACTUAL_PAYMENT_AMOUNT,
            SYSTEM_INVOICE_ATTACHMENTS,
            SYSTEM_PENDING_WRITE_OFF_AMOUNT
    );

    private final ObjectMapper objectMapper;
    private final ProcessExpenseTypeMapper processExpenseTypeMapper;

    public Map<String, Object> readSchema(String schemaJson, String detailType) {
        if (trimToNull(schemaJson) == null) {
            return normalizeSchema(defaultSchema(), detailType);
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(schemaJson, new TypeReference<LinkedHashMap<String, Object>>() {});
            return normalizeSchema(raw, detailType);
        } catch (Exception ex) {
            throw new IllegalStateException("璐圭敤鏄庣粏琛ㄥ崟 schema 瑙ｆ瀽澶辫触", ex);
        }
    }

    public String writeSchema(Map<String, Object> schema, String detailType) {
        try {
            return objectMapper.writeValueAsString(normalizeSchema(schema, detailType));
        } catch (Exception ex) {
            throw new IllegalStateException("璐圭敤鏄庣粏琛ㄥ崟 schema 搴忓垪鍖栧け璐?", ex);
        }
    }

    public Map<String, Object> normalizeSchema(Map<String, Object> schema, String detailType) {
        String normalizedDetailType = normalizeDetailType(detailType);
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("layoutMode", readLayoutMode(schema));

        List<Map<String, Object>> rawBlocks = readBlocks(schema);
        List<String> expectedSystemFieldOrder = expectedSystemFieldOrder(normalizedDetailType);
        Set<String> expectedSystemFields = new LinkedHashSet<>(expectedSystemFieldOrder);

        Map<String, Map<String, Object>> normalizedSystemBlocks = new LinkedHashMap<>();
        List<Map<String, Object>> nextBlocks = new ArrayList<>();

        for (Map<String, Object> rawBlock : rawBlocks) {
            String systemFieldCode = readSystemFieldCode(rawBlock);
            if (expectedSystemFields.contains(systemFieldCode)) {
                Map<String, Object> systemBlock = normalizeSystemBlock(rawBlock, systemFieldCode, normalizedDetailType);
                if (!normalizedSystemBlocks.containsKey(systemFieldCode)) {
                    normalizedSystemBlocks.put(systemFieldCode, systemBlock);
                    nextBlocks.add(systemBlock);
                }
                continue;
            }
            nextBlocks.add(normalizeGenericBlock(rawBlock));
        }

        for (String systemFieldCode : expectedSystemFieldOrder) {
            if (normalizedSystemBlocks.containsKey(systemFieldCode)) {
                continue;
            }
            nextBlocks.add(createSystemBlock(systemFieldCode, normalizedDetailType));
        }

        normalized.put("blocks", nextBlocks);
        return normalized;
    }

    public Map<String, Object> defaultSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("layoutMode", "TWO_COLUMN");
        schema.put("blocks", Collections.emptyList());
        return schema;
    }

    public List<ProcessFormOptionVO> loadExpenseTypeOptions() {
        return loadEnabledExpenseTypes().stream().map(item -> {
            ProcessFormOptionVO option = new ProcessFormOptionVO();
            option.setValue(item.getExpenseCode());
            option.setLabel(item.getExpenseName());
            return option;
        }).toList();
    }

    public Map<String, String> loadExpenseTypeInvoiceFreeModeMap() {
        Map<String, String> result = new LinkedHashMap<>();
        loadEnabledExpenseTypes().forEach(item -> result.put(item.getExpenseCode(), trimToNull(item.getInvoiceFreeMode())));
        return result;
    }

    public String normalizeDetailType(String detailType) {
        return Objects.equals(trimToNull(detailType), DETAIL_TYPE_ENTERPRISE) ? DETAIL_TYPE_ENTERPRISE : DETAIL_TYPE_NORMAL;
    }

    private List<ProcessExpenseType> loadEnabledExpenseTypes() {
        return processExpenseTypeMapper.selectList(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .eq(ProcessExpenseType::getStatus, 1)
                        .orderByAsc(ProcessExpenseType::getExpenseCode, ProcessExpenseType::getId)
        );
    }

    private String readLayoutMode(Map<String, Object> schema) {
        String layoutMode = schema == null ? null : trimToNull(String.valueOf(schema.get("layoutMode")));
        return layoutMode == null ? "TWO_COLUMN" : layoutMode;
    }

    private List<Map<String, Object>> readBlocks(Map<String, Object> schema) {
        Object rawBlocks = schema == null ? null : schema.get("blocks");
        if (!(rawBlocks instanceof Collection<?> blocks)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : blocks) {
            if (item instanceof Map<?, ?> blockMap) {
                result.add(toStringObjectMap(blockMap));
            }
        }
        return result;
    }

    private Map<String, Object> normalizeGenericBlock(Map<String, Object> rawBlock) {
        Map<String, Object> block = new LinkedHashMap<>(rawBlock);
        block.put("props", copyMap(rawBlock.get("props")));
        block.put("permission", copyMap(rawBlock.get("permission")));
        block.put("span", normalizeSpan(rawBlock.get("span")));
        block.put("required", Boolean.TRUE.equals(rawBlock.get("required")));
        block.put("helpText", stringValue(rawBlock.get("helpText")));
        return block;
    }

    private Map<String, Object> normalizeSystemBlock(Map<String, Object> rawBlock, String systemFieldCode, String detailType) {
        Map<String, Object> block = createSystemBlock(systemFieldCode, detailType);
        block.put("blockId", firstNonBlank(stringValue(rawBlock.get("blockId")), stringValue(block.get("blockId"))));
        block.put("span", normalizeSpan(rawBlock.get("span")));
        block.put("helpText", stringValue(rawBlock.get("helpText")));
        block.put("permission", readPermission(rawBlock.get("permission")));
        return block;
    }

    private Map<String, Object> createSystemBlock(String systemFieldCode, String detailType) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("blockId", "system-" + SYSTEM_FIELD_KEYS.get(systemFieldCode));
        block.put("fieldKey", SYSTEM_FIELD_KEYS.get(systemFieldCode));
        block.put("kind", "CONTROL");
        block.put("label", systemFieldLabel(systemFieldCode));
        block.put("span", 1);
        block.put("required", isSystemFieldAlwaysRequired(systemFieldCode, detailType));
        block.put("helpText", "");
        block.put("defaultValue", systemFieldDefaultValue(systemFieldCode, detailType));
        block.put("props", buildSystemFieldProps(systemFieldCode, detailType));
        block.put("permission", defaultPermission());
        return block;
    }

    private Map<String, Object> buildSystemFieldProps(String systemFieldCode, String detailType) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("systemFieldCode", systemFieldCode);
        props.put("locked", Boolean.TRUE);
        props.put("readOnly", Objects.equals(systemFieldCode, SYSTEM_BUSINESS_SCENARIO) && Objects.equals(detailType, DETAIL_TYPE_NORMAL));

        if (Objects.equals(systemFieldCode, SYSTEM_EXPENSE_TYPE)) {
            props.put("controlType", CONTROL_TYPE_SELECT);
            props.put("placeholder", "璇烽€夋嫨璐圭敤绫诲瀷");
            props.put("options", loadExpenseTypeOptionMaps());
            return props;
        }
        if (Objects.equals(systemFieldCode, SYSTEM_BUSINESS_SCENARIO)) {
            props.put("controlType", CONTROL_TYPE_SELECT);
            props.put("placeholder", "璇烽€夋嫨涓氬姟鍦烘櫙");
            props.put("options", Objects.equals(detailType, DETAIL_TYPE_ENTERPRISE)
                    ? List.of(option("鍒扮エ鍏ㄩ儴鏀粯", MODE_INVOICE_FULL_PAYMENT), option("棰勪粯鏈埌绁?", MODE_PREPAY_UNBILLED))
                    : List.of(option("鍒扮エ鍏ㄩ儴鏀粯", MODE_INVOICE_FULL_PAYMENT)));
            return props;
        }
        if (Objects.equals(systemFieldCode, SYSTEM_INVOICE_AMOUNT)
                || Objects.equals(systemFieldCode, SYSTEM_ACTUAL_PAYMENT_AMOUNT)
                || Objects.equals(systemFieldCode, SYSTEM_PENDING_WRITE_OFF_AMOUNT)) {
            props.put("controlType", CONTROL_TYPE_AMOUNT);
            props.put("placeholder", "璇疯緭鍏ラ噾棰?");
            props.put("precision", 2);
            if (Objects.equals(detailType, DETAIL_TYPE_ENTERPRISE)) {
                if (Objects.equals(systemFieldCode, SYSTEM_INVOICE_AMOUNT)) {
                    props.put("visibleSceneModes", List.of(MODE_INVOICE_FULL_PAYMENT));
                } else if (Objects.equals(systemFieldCode, SYSTEM_PENDING_WRITE_OFF_AMOUNT)) {
                    props.put("visibleSceneModes", List.of(MODE_PREPAY_UNBILLED));
                }
            }
            return props;
        }
        if (Objects.equals(systemFieldCode, SYSTEM_INVOICE_ATTACHMENTS)) {
            props.put("controlType", CONTROL_TYPE_ATTACHMENT);
            props.put("maxCount", 20);
            props.put("maxSizeMb", 10);
            props.put("accept", ".pdf,.jpg,.jpeg,.png,.webp");
            if (Objects.equals(detailType, DETAIL_TYPE_ENTERPRISE)) {
                props.put("visibleSceneModes", List.of(MODE_INVOICE_FULL_PAYMENT));
            }
        }
        return props;
    }

    private List<Map<String, Object>> loadExpenseTypeOptionMaps() {
        return loadExpenseTypeOptions().stream().map(item -> option(item.getLabel(), item.getValue())).toList();
    }

    private Map<String, Object> option(String label, String value) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("label", label);
        option.put("value", value);
        return option;
    }

    private List<String> expectedSystemFieldOrder(String detailType) {
        return Objects.equals(detailType, DETAIL_TYPE_ENTERPRISE) ? ENTERPRISE_SYSTEM_FIELD_ORDER : NORMAL_SYSTEM_FIELD_ORDER;
    }

    private boolean isSystemFieldAlwaysRequired(String systemFieldCode, String detailType) {
        if (Objects.equals(systemFieldCode, SYSTEM_EXPENSE_TYPE)
                || Objects.equals(systemFieldCode, SYSTEM_BUSINESS_SCENARIO)
                || Objects.equals(systemFieldCode, SYSTEM_ACTUAL_PAYMENT_AMOUNT)) {
            return true;
        }
        return Objects.equals(detailType, DETAIL_TYPE_NORMAL) && Objects.equals(systemFieldCode, SYSTEM_INVOICE_AMOUNT);
    }

    private Object systemFieldDefaultValue(String systemFieldCode, String detailType) {
        if (Objects.equals(systemFieldCode, SYSTEM_BUSINESS_SCENARIO) && Objects.equals(detailType, DETAIL_TYPE_NORMAL)) {
            return MODE_INVOICE_FULL_PAYMENT;
        }
        if (Objects.equals(systemFieldCode, SYSTEM_INVOICE_ATTACHMENTS)) {
            return new ArrayList<>();
        }
        return null;
    }

    private String systemFieldLabel(String systemFieldCode) {
        return switch (systemFieldCode) {
            case SYSTEM_EXPENSE_TYPE -> "璐圭敤绫诲瀷";
            case SYSTEM_BUSINESS_SCENARIO -> "涓氬姟鍦烘櫙";
            case SYSTEM_INVOICE_AMOUNT -> "鍒扮エ閲戦";
            case SYSTEM_ACTUAL_PAYMENT_AMOUNT -> "浠樻閲戦";
            case SYSTEM_INVOICE_ATTACHMENTS -> "涓婁紶鍙戠エ";
            case SYSTEM_PENDING_WRITE_OFF_AMOUNT -> "鏈埌绁ㄩ噾棰?";
            default -> "绯荤粺瀛楁";
        };
    }

    private int normalizeSpan(Object value) {
        return Objects.equals(value, 2) ? 2 : 1;
    }

    private String readSystemFieldCode(Map<String, Object> block) {
        Object rawProps = block.get("props");
        if (!(rawProps instanceof Map<?, ?> props)) {
            return null;
        }
        return trimToNull(String.valueOf(props.get("systemFieldCode")));
    }

    private Map<String, Object> readPermission(Object value) {
        Map<String, Object> permission = copyMap(value);
        if (!permission.containsKey("fixedStages")) {
            permission.putAll(defaultPermission());
        }
        return permission;
    }

    private Map<String, Object> defaultPermission() {
        Map<String, Object> fixedStages = new LinkedHashMap<>();
        fixedStages.put("DRAFT_BEFORE_SUBMIT", "EDITABLE");
        fixedStages.put("RESUBMIT_AFTER_RETURN", "EDITABLE");
        fixedStages.put("IN_APPROVAL", "READONLY");
        fixedStages.put("ARCHIVED", "READONLY");

        Map<String, Object> permission = new LinkedHashMap<>();
        permission.put("fixedStages", fixedStages);
        permission.put("sceneOverrides", new ArrayList<>());
        return permission;
    }

    private Map<String, Object> copyMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return new LinkedHashMap<>();
        }
        return toStringObjectMap(map);
    }

    private Map<String, Object> toStringObjectMap(Map<?, ?> rawMap) {
        Map<String, Object> result = new LinkedHashMap<>();
        rawMap.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
