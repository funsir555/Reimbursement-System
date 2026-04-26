package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.ProcessDocumentTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
class ExpenseDocumentMetadataSupport {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int PM_TITLE_MAX_LENGTH = 128;

    Map<String, Object> buildSubmitPayload(ProcessDocumentTemplate template) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("templateCode", template.getTemplateCode());
        payload.put("templateName", defaultText(template.getTemplateName(), template.getTemplateCode()));
        return payload;
    }

    String resolveDocumentTitle(ProcessDocumentTemplate template, Map<String, Object> formData, String username) {
        String title = firstNonBlank(
                stringValue(formData.get("__documentTitle")),
                stringValue(formData.get("documentTitle")),
                stringValue(formData.get("title"))
        );
        String resolved = title != null
                ? title
                : template.getTemplateName() + "-" + defaultUsername(username) + "-" + LocalDate.now().format(DATE_FORMATTER);
        validatePmTitleLength(resolved, "单据标题");
        return resolved;
    }

    String resolveDocumentReason(ProcessDocumentTemplate template, Map<String, Object> formData) {
        String reason = firstNonBlank(
                stringValue(formData.get("__documentReason")),
                stringValue(formData.get("documentReason")),
                stringValue(formData.get("reason")),
                stringValue(formData.get("summary")),
                stringValue(formData.get("bankPushSummary"))
        );
        return reason == null ? defaultReason(template.getTemplateName()) : reason;
    }

    private void validatePmTitleLength(String value, String fieldName) {
        if (value != null && value.length() > PM_TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException(fieldName + "长度不能超过" + PM_TITLE_MAX_LENGTH + "个字符");
        }
    }

    private String defaultReason(String templateName) {
        return defaultText(templateName, "报销单据");
    }

    private String defaultUsername(String username) {
        return defaultText(trimToNull(username), "用户");
    }

    private String stringValue(Object value) {
        return value == null ? null : trimToNull(String.valueOf(value));
    }

    private String firstNonBlank(String... values) {
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

    private String defaultText(String value, String defaultValue) {
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
}
