package com.finex.auth.service.impl.expense;

import com.finex.auth.service.impl.ExpenseDetailSystemFieldSupport;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Decides whether a generic form attachment references the requested stored file. */
final class ExpenseAttachmentReferencePolicy {

    private static final Pattern LEGACY_CONTENT_URL = Pattern.compile(
            "(?:^|/)(?:api/)?auth/expenses/attachments/([A-Za-z0-9]{16,64})/content(?:$|[?#])",
            Pattern.CASE_INSENSITIVE
    );

    private ExpenseAttachmentReferencePolicy() {
    }

    static boolean contains(Map<String, Object> schema, Map<String, Object> formData, String attachmentId) {
        Object rawBlocks = schema.get("blocks");
        if (!(rawBlocks instanceof List<?> blocks) || blocks.isEmpty()) {
            return false;
        }
        for (Object rawBlock : blocks) {
            Map<String, Object> block = objectMap(rawBlock);
            if (!Objects.equals(text(block.get("kind")), "CONTROL")) {
                continue;
            }
            Map<String, Object> props = objectMap(block.get("props"));
            String controlType = text(props.get("controlType"));
            if (!Objects.equals(controlType, "ATTACHMENT") && !Objects.equals(controlType, "IMAGE")) {
                continue;
            }
            String fieldKey = text(block.get("fieldKey"));
            if (fieldKey == null || Objects.equals(fieldKey, ExpenseDetailSystemFieldSupport.FIELD_INVOICE_ATTACHMENTS)) {
                continue;
            }
            if (containsId(formData.get(fieldKey), attachmentId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsId(Object value, String attachmentId) {
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (containsId(item, attachmentId)) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = objectMap(map);
            return Objects.equals(text(normalized.get("attachmentId")), attachmentId)
                    || Objects.equals(text(normalized.get("id")), attachmentId)
                    || containsLegacyUrl(normalized.get("previewUrl"), attachmentId)
                    || containsLegacyUrl(normalized.get("fileUrl"), attachmentId)
                    || containsLegacyUrl(normalized.get("url"), attachmentId);
        }
        return false;
    }

    private static boolean containsLegacyUrl(Object value, String attachmentId) {
        String url = text(value);
        if (url == null) {
            return false;
        }
        Matcher matcher = LEGACY_CONTENT_URL.matcher(url);
        return matcher.find() && Objects.equals(text(matcher.group(1)), attachmentId);
    }

    private static Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return new LinkedHashMap<>();
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
