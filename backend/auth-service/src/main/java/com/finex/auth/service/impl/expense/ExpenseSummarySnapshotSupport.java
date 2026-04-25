package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class ExpenseSummarySnapshotSupport extends AbstractExpenseSummarySupport {

    ExpenseSummarySnapshotSupport(ExpenseSummarySupportContext context) {
        super(context);
    }

    ExpenseSummaryAssembler.SummaryDraft buildDraft(
            ProcessDocumentInstance instance,
            List<ProcessDocumentExpenseDetail> expenseDetails,
            String tagArchiveCode
    ) {
        Map<String, Object> formData = readMap(instance.getFormDataJson());
        Map<String, Object> schema = readSchema(instance.getFormSchemaSnapshotJson());
        ExpenseSummaryAssembler.SummaryDraft draft = new ExpenseSummaryAssembler.SummaryDraft();
        draft.setDocumentCode(instance.getDocumentCode());
        draft.setPaymentCompanyId(extractFirstBusinessComponentValue(schema, formData, PAYMENT_COMPANY_COMPONENT_CODE));
        draft.setPayeeValue(extractFirstBusinessComponentValue(schema, formData, PAYEE_COMPONENT_CODE));
        draft.setCounterpartyValue(extractFirstBusinessComponentValue(schema, formData, COUNTERPARTY_COMPONENT_CODE));
        draft.setPaymentDate(extractPaymentDate(schema, formData));
        draft.setUndertakeDepartmentIds(resolveUndertakeDeptIdsFromSnapshots(schema, formData, expenseDetails));
        draft.setTagArchiveCode(tagArchiveCode);
        draft.setTagValues(tagArchiveCode == null ? Collections.emptyList() : extractArchiveValues(schema, formData, tagArchiveCode));
        return draft;
    }

    void collectLookupIds(
            ProcessDocumentInstance instance,
            ExpenseSummaryAssembler.SummaryDraft draft,
            Set<Long> userIds,
            Set<String> companyIds,
            Set<String> vendorCodes,
            Set<String> departmentIds,
            Set<String> archiveCodes
    ) {
        if (instance != null && instance.getSubmitterUserId() != null) {
            userIds.add(instance.getSubmitterUserId());
        }
        if (draft == null) {
            return;
        }
        if (draft.getPaymentCompanyId() != null) {
            companyIds.add(draft.getPaymentCompanyId());
        }
        collectPartyLookupIds(draft.getPayeeValue(), userIds, vendorCodes);
        collectVendorCode(draft.getCounterpartyValue(), vendorCodes);
        departmentIds.addAll(draft.getUndertakeDepartmentIds());
        if (draft.getTagArchiveCode() != null) {
            archiveCodes.add(draft.getTagArchiveCode());
        }
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
}
