package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ExpenseSummaryAssembler {

    private final ExpenseSummaryEnrichmentSupport enrichmentSupport;
    private final ExpenseSummaryViewSupport viewSupport;

    public ExpenseSummaryAssembler(
            ProcessDocumentActionLogMapper processDocumentActionLogMapper,
            ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper,
            ProcessDocumentTemplateMapper templateMapper,
            ProcessTemplateScopeMapper processTemplateScopeMapper,
            ProcessCustomArchiveDesignMapper customArchiveDesignMapper,
            ProcessCustomArchiveItemMapper customArchiveItemMapper,
            UserMapper userMapper,
            SystemCompanyMapper systemCompanyMapper,
            FinanceVendorMapper financeVendorMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            ObjectMapper objectMapper
    ) {
        ExpenseSummarySupportContext context = new ExpenseSummarySupportContext(
                processDocumentActionLogMapper,
                processDocumentExpenseDetailMapper,
                templateMapper,
                processTemplateScopeMapper,
                customArchiveDesignMapper,
                customArchiveItemMapper,
                userMapper,
                systemCompanyMapper,
                financeVendorMapper,
                systemDepartmentMapper,
                objectMapper
        );
        ExpenseSummaryLookupSupport lookupSupport = new ExpenseSummaryLookupSupport(context);
        ExpenseSummarySnapshotSupport snapshotSupport = new ExpenseSummarySnapshotSupport(context);
        this.enrichmentSupport = new ExpenseSummaryEnrichmentSupport(context, lookupSupport, snapshotSupport);
        this.viewSupport = new ExpenseSummaryViewSupport(context, this.enrichmentSupport);
    }

    public List<ExpenseSummaryVO> toExpenseSummaries(List<ProcessDocumentInstance> instances) {
        return viewSupport.toExpenseSummaries(instances);
    }

    public List<ExpenseApprovalPendingItemVO> toPendingItems(
            List<ProcessDocumentTask> tasks,
            Map<String, ProcessDocumentInstance> instanceMap
    ) {
        return viewSupport.toPendingItems(tasks, instanceMap);
    }

    SummaryEnrichmentData buildSummaryEnrichmentData(List<ProcessDocumentInstance> instances) {
        return enrichmentSupport.buildSummaryEnrichmentData(instances);
    }

    static final class SummaryEnrichmentData {
        static final SummaryEnrichmentData EMPTY = new SummaryEnrichmentData(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap()
        );
        private final Map<String, SummaryMetadata> metadataByDocumentCode;
        private final Map<String, LocalDateTime> submittedAtByDocumentCode;
        private final Map<String, Boolean> draftDeletableByDocumentCode;

        SummaryEnrichmentData(
                Map<String, SummaryMetadata> metadataByDocumentCode,
                Map<String, LocalDateTime> submittedAtByDocumentCode,
                Map<String, Boolean> draftDeletableByDocumentCode
        ) {
            this.metadataByDocumentCode = metadataByDocumentCode;
            this.submittedAtByDocumentCode = submittedAtByDocumentCode;
            this.draftDeletableByDocumentCode = draftDeletableByDocumentCode;
        }

        static SummaryEnrichmentData empty() {
            return EMPTY;
        }

        SummaryMetadata metadata(String documentCode) {
            return metadataByDocumentCode.getOrDefault(documentCode, SummaryMetadata.empty());
        }

        LocalDateTime submittedAt(String documentCode, ProcessDocumentInstance instance) {
            return submittedAtByDocumentCode.getOrDefault(documentCode, instance == null ? null : instance.getCreatedAt());
        }

        boolean draftDeletable(String documentCode) {
            return Boolean.TRUE.equals(draftDeletableByDocumentCode.get(documentCode));
        }
    }

    static final class SummaryMetadata {
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

        SummaryMetadata(
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

        static SummaryMetadata empty() {
            return EMPTY;
        }

        String submitterDeptName() {
            return submitterDeptName;
        }

        String paymentCompanyId() {
            return paymentCompanyId;
        }

        String paymentCompanyName() {
            return paymentCompanyName;
        }

        String payeeName() {
            return payeeName;
        }

        String counterpartyName() {
            return counterpartyName;
        }

        String paymentDate() {
            return paymentDate;
        }

        List<String> undertakeDepartmentNames() {
            return undertakeDepartmentNames;
        }

        List<String> tagNames() {
            return tagNames;
        }
    }

    static final class SummaryDraft {
        private String documentCode;
        private String paymentCompanyId;
        private String payeeValue;
        private String counterpartyValue;
        private String paymentDate;
        private List<String> undertakeDepartmentIds = Collections.emptyList();
        private String tagArchiveCode;
        private List<String> tagValues = Collections.emptyList();

        void setDocumentCode(String documentCode) {
            this.documentCode = documentCode;
        }

        void setPaymentCompanyId(String paymentCompanyId) {
            this.paymentCompanyId = paymentCompanyId;
        }

        void setPayeeValue(String payeeValue) {
            this.payeeValue = payeeValue;
        }

        void setCounterpartyValue(String counterpartyValue) {
            this.counterpartyValue = counterpartyValue;
        }

        void setPaymentDate(String paymentDate) {
            this.paymentDate = paymentDate;
        }

        void setUndertakeDepartmentIds(List<String> undertakeDepartmentIds) {
            this.undertakeDepartmentIds = undertakeDepartmentIds == null ? Collections.emptyList() : undertakeDepartmentIds;
        }

        void setTagArchiveCode(String tagArchiveCode) {
            this.tagArchiveCode = tagArchiveCode;
        }

        void setTagValues(List<String> tagValues) {
            this.tagValues = tagValues == null ? Collections.emptyList() : tagValues;
        }

        String getDocumentCode() {
            return documentCode;
        }

        String getPaymentCompanyId() {
            return paymentCompanyId;
        }

        String getPayeeValue() {
            return payeeValue;
        }

        String getCounterpartyValue() {
            return counterpartyValue;
        }

        String getPaymentDate() {
            return paymentDate;
        }

        List<String> getUndertakeDepartmentIds() {
            return undertakeDepartmentIds;
        }

        String getTagArchiveCode() {
            return tagArchiveCode;
        }

        List<String> getTagValues() {
            return tagValues;
        }
    }
}
