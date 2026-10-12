package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/** 将主表单和费用明细中的共享档案字段投影到审批条件上下文。 */
final class ExpenseSharedArchiveConditionResolver {

    private ExpenseSharedArchiveConditionResolver() {
    }

    static void putForDesign(
            Map<String, Object> context,
            Map<String, Object> mainSchema,
            Map<String, Object> mainFormData,
            Map<String, Object> detailSchema,
            List<ExpenseDetailInstanceDTO> expenseDetails
    ) {
        Set<String> archiveCodes = new LinkedHashSet<>();
        collectSharedArchiveCodes(archiveCodes, mainSchema);
        collectSharedArchiveCodes(archiveCodes, detailSchema);
        if (archiveCodes.isEmpty()) {
            return;
        }
        for (String archiveCode : archiveCodes) {
            LinkedHashSet<String> values = new LinkedHashSet<>();
            collectArchiveValues(values, mainSchema, mainFormData, archiveCode);
            if (detailSchema != null && expenseDetails != null) {
                for (ExpenseDetailInstanceDTO expenseDetail : expenseDetails) {
                    collectArchiveValues(values, detailSchema,
                            expenseDetail == null || expenseDetail.getFormData() == null
                                    ? Collections.emptyMap() : expenseDetail.getFormData(), archiveCode);
                }
            }
            context.put(archiveCode, new ArrayList<>(values));
        }
    }

    static void putForSnapshots(
            Map<String, Object> context,
            Map<String, Object> mainSchema,
            Map<String, Object> mainFormData,
            List<ProcessDocumentExpenseDetail> expenseDetails,
            Function<String, Map<String, Object>> mapReader
    ) {
        Set<String> archiveCodes = new LinkedHashSet<>();
        collectSharedArchiveCodes(archiveCodes, mainSchema);
        if (expenseDetails != null) {
            for (ProcessDocumentExpenseDetail expenseDetail : expenseDetails) {
                collectSharedArchiveCodes(archiveCodes, mapReader.apply(expenseDetail.getSchemaSnapshotJson()));
            }
        }
        if (archiveCodes.isEmpty()) {
            return;
        }
        for (String archiveCode : archiveCodes) {
            LinkedHashSet<String> values = new LinkedHashSet<>();
            collectArchiveValues(values, mainSchema, mainFormData, archiveCode);
            if (expenseDetails != null) {
                for (ProcessDocumentExpenseDetail expenseDetail : expenseDetails) {
                    collectArchiveValues(values,
                            mapReader.apply(expenseDetail.getSchemaSnapshotJson()),
                            mapReader.apply(expenseDetail.getFormDataJson()), archiveCode);
                }
            }
            context.put(archiveCode, new ArrayList<>(values));
        }
    }

    private static void collectSharedArchiveCodes(Set<String> result, Map<String, Object> schema) {
        if (result == null || schema == null) {
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
            if (!Objects.equals(String.valueOf(blockMap.get("kind")), "SHARED_FIELD")) {
                continue;
            }
            Object rawProps = blockMap.get("props");
            if (!(rawProps instanceof Map<?, ?> props)) {
                continue;
            }
            String archiveCode = trimToNull(String.valueOf(props.get("archiveCode")));
            if (archiveCode != null) {
                result.add(archiveCode);
            }
        }
    }

    private static void collectArchiveValues(
            Set<String> result, Map<String, Object> schema, Map<String, Object> formData, String archiveCode
    ) {
        if (result == null || schema == null || formData == null || formData.isEmpty()
                || trimToNull(archiveCode) == null) {
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
            collectStringValues(result, formData.get(fieldKey));
        }
    }

    private static void collectStringValues(Set<String> result, Object value) {
        if (result == null || value == null) {
            return;
        }
        if (value instanceof Collection<?> items) {
            for (Object item : items) {
                String normalized = trimToNull(String.valueOf(item));
                if (normalized != null) {
                    result.add(normalized);
                }
            }
            return;
        }
        String normalized = trimToNull(String.valueOf(value));
        if (normalized != null) {
            result.add(normalized);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
