package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessCustomArchiveItemMapper;
import com.finex.auth.mapper.ProcessDocumentActionLogMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessTemplateScopeMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;

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

abstract class AbstractExpenseSummarySupport {

    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    protected static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    protected static final String UNDERTAKE_DEPARTMENT_COMPONENT_CODE = "undertake-department";
    protected static final String PAYMENT_COMPANY_COMPONENT_CODE = "payment-company";
    protected static final String PAYEE_COMPONENT_CODE = "payee";
    protected static final String COUNTERPARTY_COMPONENT_CODE = "counterparty";
    protected static final String PERSONAL_PAYEE_VALUE_PREFIX = "PERSONAL_PAYEE:";
    protected static final String CONTROL_TYPE_DATE = "DATE";
    protected static final Charset LEGACY_GARBLED_CHARSET = Charset.forName("GBK");
    protected static final Set<String> PAYMENT_DATE_LABELS = Set.of("\u652f\u4ed8\u65e5\u671f", "\u4ed8\u6b3e\u65e5\u671f");
    protected static final Set<String> PAYMENT_DATE_LEGACY_LABEL_ALIASES = buildLegacyUtf8AsGbkLabels(PAYMENT_DATE_LABELS);
    protected static final String TEMPLATE_SCOPE_TYPE_TAG_ARCHIVE = "TAG_ARCHIVE";
    protected static final String DOCUMENT_STATUS_APPROVED = "APPROVED";
    protected static final String DOCUMENT_STATUS_COMPLETED = "COMPLETED";
    protected static final String DOCUMENT_STATUS_DRAFT = "DRAFT";
    protected static final String DOCUMENT_STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    protected static final String DOCUMENT_STATUS_REJECTED = "REJECTED";
    protected static final String DOCUMENT_STATUS_EXCEPTION = "EXCEPTION";
    protected static final String DOCUMENT_STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    protected static final String DOCUMENT_STATUS_PAYING = "PAYING";
    protected static final String DOCUMENT_STATUS_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    protected static final String DOCUMENT_STATUS_PAYMENT_FINISHED = "PAYMENT_FINISHED";
    protected static final String DOCUMENT_STATUS_PAYMENT_EXCEPTION = "PAYMENT_EXCEPTION";
    protected static final String LOG_SUBMIT = "SUBMIT";
    protected static final String LOG_RESUBMIT = "RESUBMIT";
    protected static final String LOG_RECALL = "RECALL";

    protected final ProcessDocumentActionLogMapper processDocumentActionLogMapper;
    protected final ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    protected final ProcessDocumentTemplateMapper templateMapper;
    protected final ProcessTemplateScopeMapper processTemplateScopeMapper;
    protected final ProcessCustomArchiveDesignMapper customArchiveDesignMapper;
    protected final ProcessCustomArchiveItemMapper customArchiveItemMapper;
    protected final UserMapper userMapper;
    protected final SystemCompanyMapper systemCompanyMapper;
    protected final FinanceVendorMapper financeVendorMapper;
    protected final SystemDepartmentMapper systemDepartmentMapper;
    protected final ObjectMapper objectMapper;

    protected AbstractExpenseSummarySupport(ExpenseSummarySupportContext context) {
        this.processDocumentActionLogMapper = context.processDocumentActionLogMapper;
        this.processDocumentExpenseDetailMapper = context.processDocumentExpenseDetailMapper;
        this.templateMapper = context.templateMapper;
        this.processTemplateScopeMapper = context.processTemplateScopeMapper;
        this.customArchiveDesignMapper = context.customArchiveDesignMapper;
        this.customArchiveItemMapper = context.customArchiveItemMapper;
        this.userMapper = context.userMapper;
        this.systemCompanyMapper = context.systemCompanyMapper;
        this.financeVendorMapper = context.financeVendorMapper;
        this.systemDepartmentMapper = context.systemDepartmentMapper;
        this.objectMapper = context.objectMapper;
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

    protected Map<String, Object> readMap(String json) {
        if (trimToNull(json) == null) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse json map", ex);
        }
    }

    protected BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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
            case DOCUMENT_STATUS_PENDING_APPROVAL -> "\u5ba1\u6279\u4e2d";
            case DOCUMENT_STATUS_PENDING_PAYMENT -> "\u5f85\u652f\u4ed8";
            case DOCUMENT_STATUS_PAYING -> "\u652f\u4ed8\u4e2d";
            case DOCUMENT_STATUS_PAYMENT_COMPLETED -> "\u5df2\u652f\u4ed8";
            case DOCUMENT_STATUS_PAYMENT_FINISHED -> "\u5df2\u5b8c\u6210";
            case DOCUMENT_STATUS_PAYMENT_EXCEPTION -> "\u652f\u4ed8\u5f02\u5e38";
            case DOCUMENT_STATUS_APPROVED, DOCUMENT_STATUS_COMPLETED -> "\u5df2\u5b8c\u6210";
            case DOCUMENT_STATUS_REJECTED -> "\u5df2\u9a73\u56de";
            case DOCUMENT_STATUS_DRAFT -> "\u8349\u7a3f";
            case DOCUMENT_STATUS_EXCEPTION -> "\u6d41\u7a0b\u5f02\u5e38";
            default -> "\u672a\u77e5\u72b6\u6001";
        };
    }

    protected String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    protected String defaultReason(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "\u6682\u65e0\u4e8b\u7531" : normalized;
    }

    protected String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    protected String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    protected String trimObjectToNull(Object value) {
        return trimToNull(value == null ? null : String.valueOf(value));
    }

    protected Long toLong(String value) {
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

    protected String firstStringValue(Object value) {
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

    protected String firstLookupValue(Object value) {
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

    protected String extractLookupValue(Object value) {
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

    protected void collectStringValues(Set<String> result, Object value) {
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

    protected LocalDateTime resolveDisplaySubmittedAt(com.finex.auth.entity.ProcessDocumentInstance instance, LocalDateTime latestSubmitAt) {
        if (instance == null) {
            return null;
        }
        if (Objects.equals(trimToNull(instance.getStatus()), DOCUMENT_STATUS_DRAFT)) {
            return instance.getUpdatedAt() == null ? instance.getCreatedAt() : instance.getUpdatedAt();
        }
        return latestSubmitAt == null ? instance.getCreatedAt() : latestSubmitAt;
    }

    protected boolean isPaymentDateLabel(String label) {
        String normalized = trimToNull(label);
        return normalized != null && (PAYMENT_DATE_LABELS.contains(normalized) || PAYMENT_DATE_LEGACY_LABEL_ALIASES.contains(normalized));
    }

    protected boolean looksLikePaymentDateField(String fieldKey) {
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

    private Map<String, Object> defaultSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("layoutMode", "TWO_COLUMN");
        schema.put("blocks", Collections.emptyList());
        return schema;
    }
}