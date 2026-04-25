package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class ExpenseSummaryViewSupport extends AbstractExpenseSummarySupport {

    private final ExpenseSummaryEnrichmentSupport enrichmentSupport;

    ExpenseSummaryViewSupport(ExpenseSummarySupportContext context, ExpenseSummaryEnrichmentSupport enrichmentSupport) {
        super(context);
        this.enrichmentSupport = enrichmentSupport;
    }

    List<ExpenseSummaryVO> toExpenseSummaries(List<ProcessDocumentInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return Collections.emptyList();
        }
        ExpenseSummaryAssembler.SummaryEnrichmentData enrichmentData = enrichmentSupport.buildSummaryEnrichmentData(instances);
        return instances.stream().map(instance -> toExpenseSummary(instance, enrichmentData)).toList();
    }

    List<ExpenseApprovalPendingItemVO> toPendingItems(
            List<ProcessDocumentTask> tasks,
            Map<String, ProcessDocumentInstance> instanceMap
    ) {
        if (tasks == null || tasks.isEmpty() || instanceMap == null || instanceMap.isEmpty()) {
            return Collections.emptyList();
        }
        ExpenseSummaryAssembler.SummaryEnrichmentData enrichmentData = enrichmentSupport.buildSummaryEnrichmentData(
                new ArrayList<>(instanceMap.values())
        );
        return tasks.stream().map(task -> toPendingItem(task, instanceMap.get(task.getDocumentCode()), enrichmentData)).toList();
    }

    private ExpenseSummaryVO toExpenseSummary(
            ProcessDocumentInstance instance,
            ExpenseSummaryAssembler.SummaryEnrichmentData enrichmentData
    ) {
        ExpenseSummaryVO summary = new ExpenseSummaryVO();
        ExpenseSummaryAssembler.SummaryMetadata metadata = enrichmentData.metadata(instance.getDocumentCode());
        String statusLabel = resolveStatusLabel(instance.getStatus());
        summary.setDocumentCode(instance.getDocumentCode());
        summary.setNo(instance.getDocumentCode());
        summary.setType(trimToNull(instance.getTemplateName()) != null
                ? instance.getTemplateName()
                : resolveTemplateTypeLabel(instance.getTemplateType(), null));
        summary.setReason(trimToNull(instance.getDocumentReason()) != null
                ? instance.getDocumentReason()
                : defaultReason(instance.getDocumentTitle()));
        summary.setDocumentTitle(instance.getDocumentTitle());
        summary.setDocumentReason(instance.getDocumentReason());
        summary.setSubmitterName(instance.getSubmitterName());
        summary.setSubmitterDeptName(metadata.submitterDeptName());
        summary.setTemplateName(instance.getTemplateName());
        summary.setTemplateType(instance.getTemplateType());
        summary.setTemplateTypeLabel(resolveTemplateTypeLabel(instance.getTemplateType(), templateTypeLabel(instance)));
        summary.setCurrentNodeName(instance.getCurrentNodeName());
        summary.setDocumentStatus(instance.getStatus());
        summary.setDocumentStatusLabel(statusLabel);
        summary.setAmount(defaultDecimal(instance.getTotalAmount()));
        LocalDateTime displaySubmittedAt = enrichmentData.submittedAt(instance.getDocumentCode(), instance);
        summary.setDate(displaySubmittedAt == null ? "" : displaySubmittedAt.format(DATE_FORMATTER));
        summary.setStatus(statusLabel);
        summary.setSubmittedAt(formatTime(displaySubmittedAt));
        summary.setDraftDeletable(enrichmentData.draftDeletable(instance.getDocumentCode()));
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
            ExpenseSummaryAssembler.SummaryEnrichmentData enrichmentData
    ) {
        ExpenseApprovalPendingItemVO item = new ExpenseApprovalPendingItemVO();
        ExpenseSummaryAssembler.SummaryMetadata metadata = instance == null
                ? ExpenseSummaryAssembler.SummaryMetadata.empty()
                : enrichmentData.metadata(task.getDocumentCode());
        item.setTaskId(task.getId());
        item.setDocumentCode(task.getDocumentCode());
        item.setDocumentTitle(instance == null ? "" : instance.getDocumentTitle());
        item.setDocumentReason(instance == null ? "" : instance.getDocumentReason());
        item.setTemplateName(instance == null ? "" : instance.getTemplateName());
        item.setTemplateType(instance == null ? null : instance.getTemplateType());
        item.setTemplateTypeLabel(instance == null ? null : resolveTemplateTypeLabel(instance.getTemplateType(), templateTypeLabel(instance)));
        item.setSubmitterName(instance == null ? "" : instance.getSubmitterName());
        item.setSubmitterDeptName(metadata.submitterDeptName());
        item.setAmount(instance == null ? BigDecimal.ZERO : defaultDecimal(instance.getTotalAmount()));
        item.setNodeKey(task.getNodeKey());
        item.setNodeName(task.getNodeName());
        item.setStatus(task.getStatus());
        item.setDocumentStatus(instance == null ? null : instance.getStatus());
        item.setDocumentStatusLabel(instance == null ? null : resolveStatusLabel(instance.getStatus()));
        item.setSubmittedAt(instance == null ? null : formatTime(enrichmentData.submittedAt(task.getDocumentCode(), instance)));
        item.setPaymentDate(metadata.paymentDate());
        item.setPaymentCompanyName(metadata.paymentCompanyName());
        item.setPayeeName(metadata.payeeName());
        item.setCounterpartyName(metadata.counterpartyName());
        item.setUndertakeDepartmentNames(metadata.undertakeDepartmentNames());
        item.setTagNames(metadata.tagNames());
        item.setTaskCreatedAt(formatTime(task.getCreatedAt()));
        return item;
    }

    private String templateTypeLabel(ProcessDocumentInstance instance) {
        Object value = readMap(instance.getTemplateSnapshotJson()).get("templateTypeLabel");
        return value == null ? null : String.valueOf(value);
    }
}
