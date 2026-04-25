package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class ExpenseSummaryEnrichmentSupport extends AbstractExpenseSummarySupport {

    private final ExpenseSummaryLookupSupport lookupSupport;
    private final ExpenseSummarySnapshotSupport snapshotSupport;

    ExpenseSummaryEnrichmentSupport(
            ExpenseSummarySupportContext context,
            ExpenseSummaryLookupSupport lookupSupport,
            ExpenseSummarySnapshotSupport snapshotSupport
    ) {
        super(context);
        this.lookupSupport = lookupSupport;
        this.snapshotSupport = snapshotSupport;
    }

    ExpenseSummaryAssembler.SummaryEnrichmentData buildSummaryEnrichmentData(List<ProcessDocumentInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return ExpenseSummaryAssembler.SummaryEnrichmentData.empty();
        }

        List<String> documentCodes = instances.stream()
                .map(ProcessDocumentInstance::getDocumentCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, List<ProcessDocumentExpenseDetail>> expenseDetailMap = lookupSupport.loadExpenseDetailMap(documentCodes);
        ExpenseSummaryLifecycleData lifecycleData = lookupSupport.loadLifecycleData(documentCodes);

        List<String> templateCodes = instances.stream()
                .map(ProcessDocumentInstance::getTemplateCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, ProcessDocumentTemplate> templateMap = lookupSupport.loadTemplateMap(templateCodes);
        Map<String, String> tagArchiveCodeByTemplateCode = lookupSupport.loadTagArchiveCodeByTemplateCode(templateMap);

        Map<String, ExpenseSummaryAssembler.SummaryDraft> draftMap = new LinkedHashMap<>();
        Set<Long> userIds = new LinkedHashSet<>();
        Set<String> companyIds = new LinkedHashSet<>();
        Set<String> vendorCodes = new LinkedHashSet<>();
        Set<String> departmentIds = new LinkedHashSet<>();
        Set<String> archiveCodes = new LinkedHashSet<>();

        for (ProcessDocumentInstance instance : instances) {
            String documentCode = instance.getDocumentCode();
            List<ProcessDocumentExpenseDetail> expenseDetails = expenseDetailMap.getOrDefault(documentCode, Collections.emptyList());
            String tagArchiveCode = tagArchiveCodeByTemplateCode.get(instance.getTemplateCode());
            ExpenseSummaryAssembler.SummaryDraft draft = snapshotSupport.buildDraft(instance, expenseDetails, tagArchiveCode);
            draftMap.put(documentCode, draft);
            snapshotSupport.collectLookupIds(instance, draft, userIds, companyIds, vendorCodes, departmentIds, archiveCodes);
        }

        Map<Long, User> userMap = lookupSupport.loadUserMap(userIds);
        lookupSupport.addSubmitterDepartmentIds(userMap, departmentIds);
        Map<String, SystemCompany> companyMap = lookupSupport.loadCompanyMap(companyIds);
        Map<String, FinanceVendor> vendorMap = lookupSupport.loadVendorMap(vendorCodes);
        Map<String, String> departmentNameMap = lookupSupport.loadDepartmentNameMap(departmentIds);
        Map<String, Map<String, String>> archiveItemLabelMap = lookupSupport.loadArchiveItemLabelMap(archiveCodes);

        Map<String, ExpenseSummaryAssembler.SummaryMetadata> metadataMap = new LinkedHashMap<>();
        Map<String, LocalDateTime> submittedAtMap = new LinkedHashMap<>();
        Map<String, Boolean> draftDeletableMap = new LinkedHashMap<>();
        for (ProcessDocumentInstance instance : instances) {
            String documentCode = trimToNull(instance.getDocumentCode());
            if (documentCode == null) {
                continue;
            }
            ExpenseSummaryAssembler.SummaryDraft draft = draftMap.get(documentCode);
            metadataMap.put(
                    documentCode,
                    lookupSupport.buildMetadata(
                            instance.getSubmitterUserId(),
                            draft,
                            userMap,
                            companyMap,
                            vendorMap,
                            departmentNameMap,
                            archiveItemLabelMap
                    )
            );
            submittedAtMap.put(
                    documentCode,
                    resolveDisplaySubmittedAt(instance, lifecycleData.latestSubmitAtByDocumentCode().get(documentCode))
            );
            draftDeletableMap.put(
                    documentCode,
                    Objects.equals(trimToNull(instance.getStatus()), DOCUMENT_STATUS_DRAFT)
                            && !lifecycleData.formalProcessHistoryDocumentCodes().contains(documentCode)
            );
        }
        return new ExpenseSummaryAssembler.SummaryEnrichmentData(metadataMap, submittedAtMap, draftDeletableMap);
    }
}
