package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.PmBankPaymentRecord;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.SystemBankBranchCatalog;
import com.finex.auth.entity.UserBankAccount;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class ExpensePaymentOrderQuerySupport extends AbstractExpensePaymentSupport {

    ExpensePaymentOrderQuerySupport(ExpensePaymentSupportContext context) {
        super(context);
    }

    List<ExpensePaymentOrderVO> listPaymentOrders(Long userId, String status) {
        String normalizedStatus = normalizePaymentOrderStatus(status);
        List<ProcessDocumentTask> tasks = loadVisiblePaymentTasks(userId, normalizedStatus);
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> documentCodes = tasks.stream().map(ProcessDocumentTask::getDocumentCode).toList();
        Map<String, ProcessDocumentInstance> instanceMap = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .in(ProcessDocumentInstance::getDocumentCode, documentCodes)
        ).stream().collect(Collectors.toMap(
                ProcessDocumentInstance::getDocumentCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        ExpenseSummaryAssembler.SummaryEnrichmentData enrichmentData = expenseSummaryAssembler.buildSummaryEnrichmentData(
                new ArrayList<>(instanceMap.values())
        );
        Map<String, List<ProcessDocumentExpenseDetail>> expenseDetailMap = loadExpenseDetailMap(documentCodes);
        Map<String, PmBankPaymentRecord> bankRecordMap = loadLatestBankRecordMap(documentCodes);
        Map<Long, String> companyBankAccountNameMap = loadCompanyBankAccountNameMap(
                bankRecordMap.values().stream()
                        .map(PmBankPaymentRecord::getCompanyBankAccountId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );
        Map<String, PaymentReceiverInfo> receiverInfoMap = new LinkedHashMap<>();
        for (ProcessDocumentTask task : tasks) {
            ProcessDocumentInstance instance = instanceMap.get(task.getDocumentCode());
            if (instance == null) {
                continue;
            }
            receiverInfoMap.put(
                    task.getDocumentCode(),
                    resolvePaymentReceiverInfo(instance, enrichmentData.metadata(task.getDocumentCode()))
            );
        }
        Map<String, SystemBankBranchCatalog> branchCatalogMap = loadBranchCatalogMap(
                receiverInfoMap.values().stream()
                        .map(PaymentReceiverInfo::branchCode)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(HashSet::new))
        );
        return tasks.stream()
                .map(task -> toPaymentOrder(
                        task,
                        instanceMap.get(task.getDocumentCode()),
                        enrichmentData,
                        bankRecordMap.get(task.getDocumentCode()),
                        companyBankAccountNameMap,
                        receiverInfoMap.get(task.getDocumentCode()),
                        expenseDetailMap.get(task.getDocumentCode()),
                        branchCatalogMap
                ))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ProcessDocumentTask> loadVisiblePaymentTasks(Long userId, String normalizedStatus) {
        List<ProcessDocumentTask> tasks = processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getAssigneeUserId, userId)
                        .eq(ProcessDocumentTask::getNodeType, NODE_TYPE_PAYMENT)
                        .orderByDesc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        );
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, String> statusByDocumentCode = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .in(ProcessDocumentInstance::getDocumentCode, tasks.stream().map(ProcessDocumentTask::getDocumentCode).toList())
        ).stream().collect(Collectors.toMap(
                ProcessDocumentInstance::getDocumentCode,
                ProcessDocumentInstance::getStatus,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        return tasks.stream()
                .filter(task -> normalizedStatus.equals(trimToNull(statusByDocumentCode.get(task.getDocumentCode()))))
                .toList();
    }

    private ExpensePaymentOrderVO toPaymentOrder(
            ProcessDocumentTask task,
            ProcessDocumentInstance instance,
            ExpenseSummaryAssembler.SummaryEnrichmentData enrichmentData,
            PmBankPaymentRecord bankPaymentRecord,
            Map<Long, String> companyBankAccountNameMap,
            PaymentReceiverInfo receiverInfo,
            List<ProcessDocumentExpenseDetail> expenseDetails,
            Map<String, SystemBankBranchCatalog> branchCatalogMap
    ) {
        if (instance == null) {
            return null;
        }
        ExpenseSummaryAssembler.SummaryMetadata metadata = enrichmentData.metadata(task.getDocumentCode());
        ExpensePaymentOrderVO item = new ExpensePaymentOrderVO();
        item.setTaskId(task.getId());
        item.setDocumentCode(task.getDocumentCode());
        item.setDocumentTitle(instance.getDocumentTitle());
        item.setTemplateName(instance.getTemplateName());
        item.setTemplateType(instance.getTemplateType());
        item.setTemplateTypeLabel(resolveTemplateTypeLabel(
                instance.getTemplateType(),
                readMap(instance.getTemplateSnapshotJson()).get("templateTypeLabel") == null
                        ? null
                        : String.valueOf(readMap(instance.getTemplateSnapshotJson()).get("templateTypeLabel"))
        ));
        item.setSubmitterName(instance.getSubmitterName());
        item.setSubmitterDeptName(metadata.submitterDeptName());
        item.setCurrentNodeName(firstNonBlank(instance.getCurrentNodeName(), task.getNodeName()));
        item.setDocumentStatus(instance.getStatus());
        item.setDocumentStatusLabel(resolveStatusLabel(instance.getStatus()));
        item.setAmount(instance.getTotalAmount());
        item.setSubmittedAt(formatTime(enrichmentData.submittedAt(instance.getDocumentCode(), instance)));
        item.setPaymentDate(metadata.paymentDate());
        item.setPaymentCompanyName(metadata.paymentCompanyName());
        item.setPaymentStatusCode(instance.getStatus());
        item.setPaymentStatusLabel(resolveStatusLabel(instance.getStatus()));
        item.setManualPaid(bankPaymentRecord != null && isFlagEnabled(bankPaymentRecord.getManualPaid()));
        item.setPaidAt(bankPaymentRecord == null ? null : formatTime(bankPaymentRecord.getPaidAt()));
        item.setReceiptStatusLabel(resolveReceiptStatusLabel(bankPaymentRecord));
        item.setReceiptReceivedAt(bankPaymentRecord == null ? null : formatTime(bankPaymentRecord.getReceiptReceivedAt()));
        item.setBankFlowNo(bankPaymentRecord == null ? null : bankPaymentRecord.getBankFlowNo());
        item.setCompanyBankAccountName(bankPaymentRecord == null ? null : companyBankAccountNameMap.get(bankPaymentRecord.getCompanyBankAccountId()));
        item.setTaskCreatedAt(formatTime(task.getCreatedAt()));
        item.setAllowRetry(expenseWorkflowRuntimeSupport.paymentTaskAllowsRetry(instance, task));
        PaymentReceiverInfo effectiveReceiverInfo = receiverInfo == null
                ? resolvePaymentReceiverInfo(instance, metadata)
                : receiverInfo;
        item.setPayeeOrCounterpartyName(effectiveReceiverInfo.receiverName());
        item.setPayeeAccountNo(effectiveReceiverInfo.accountNo());
        item.setPayeeBankName(effectiveReceiverInfo.bankName());
        item.setActualPaymentAmount(resolveActualPaymentAmount(expenseDetails));
        item.setBankPushSummary(resolveBankPushSummary(instance));
        item.setPayeeBankProvince(resolvePayeeBankProvince(effectiveReceiverInfo, branchCatalogMap));
        item.setPayeeBankCity(resolvePayeeBankCity(effectiveReceiverInfo, branchCatalogMap));
        return item;
    }

    private PaymentReceiverInfo resolvePaymentReceiverInfo(
            ProcessDocumentInstance instance,
            ExpenseSummaryAssembler.SummaryMetadata metadata
    ) {
        String receiverName = metadata == null ? null : metadata.counterpartyName();
        Map<String, Object> schema = readSchema(instance.getFormSchemaSnapshotJson());
        Map<String, Object> formData = readMap(instance.getFormDataJson());
        Object payeeAccountRawValue = extractFirstBusinessComponentRawValue(schema, formData, PAYEE_ACCOUNT_COMPONENT_CODE);
        PaymentReceiverInfo accountInfo = resolvePayeeAccountInfo(
                payeeAccountRawValue,
                metadata == null ? null : metadata.paymentCompanyId()
        );
        if (receiverName == null) {
            receiverName = firstNonBlank(
                    accountInfo.receiverName(),
                    accountInfo.accountName(),
                    metadata == null ? null : metadata.payeeName()
            );
        }
        return new PaymentReceiverInfo(
                receiverName,
                accountInfo.accountNo(),
                accountInfo.bankName(),
                accountInfo.accountName(),
                accountInfo.branchCode(),
                accountInfo.province(),
                accountInfo.city()
        );
    }

    private PaymentReceiverInfo resolvePayeeAccountInfo(Object rawValue, String paymentCompanyId) {
        if (rawValue instanceof Map<?, ?> map) {
            String sourceType = trimObjectToNull(map.get("sourceType"));
            String value = firstNonBlank(
                    trimObjectToNull(map.get("value")),
                    trimObjectToNull(map.get("sourceCode")),
                    trimObjectToNull(map.get("ownerCode"))
            );
            PaymentReceiverInfo snapshotInfo = new PaymentReceiverInfo(
                    firstNonBlank(trimObjectToNull(map.get("ownerName")), trimObjectToNull(map.get("accountName"))),
                    trimObjectToNull(map.get("accountNo")),
                    trimObjectToNull(map.get("bankName")),
                    trimObjectToNull(map.get("accountName")),
                    trimObjectToNull(map.get("branchCode")),
                    trimObjectToNull(map.get("province")),
                    trimObjectToNull(map.get("city"))
            );
            PaymentReceiverInfo resolved = resolvePayeeAccountInfoBySource(sourceType, value, paymentCompanyId);
            if (!resolved.isEmpty()) {
                return resolved.merge(snapshotInfo);
            }
            return snapshotInfo.withAccountNo(firstNonBlank(snapshotInfo.accountNo(), trimObjectToNull(map.get("accountNoMasked"))));
        }
        if (rawValue instanceof List<?> items) {
            for (Object item : items) {
                PaymentReceiverInfo resolved = resolvePayeeAccountInfo(item, paymentCompanyId);
                if (!resolved.isEmpty()) {
                    return resolved;
                }
            }
            return emptyPaymentReceiverInfo();
        }
        return resolvePayeeAccountInfoBySource(
                null,
                trimToNull(rawValue == null ? null : String.valueOf(rawValue)),
                paymentCompanyId
        );
    }

    private PaymentReceiverInfo resolvePayeeAccountInfoBySource(String sourceType, String value, String paymentCompanyId) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            return emptyPaymentReceiverInfo();
        }
        String normalizedSourceType = trimToNull(sourceType);
        if ("VENDOR".equalsIgnoreCase(normalizedSourceType) || normalizedValue.startsWith("VENDOR:")) {
            return resolveVendorReceiverInfo(normalizedValue, paymentCompanyId);
        }
        if ("USER".equalsIgnoreCase(normalizedSourceType) || normalizedValue.startsWith("USER_ACCOUNT:")) {
            return resolveUserReceiverInfo(normalizedValue);
        }
        return emptyPaymentReceiverInfo();
    }

    private PaymentReceiverInfo resolveVendorReceiverInfo(String value, String paymentCompanyId) {
        String vendorCode = trimToNull(value);
        if (vendorCode != null && vendorCode.startsWith("VENDOR:")) {
            vendorCode = trimToNull(vendorCode.substring("VENDOR:".length()));
        }
        if (vendorCode == null) {
            return emptyPaymentReceiverInfo();
        }
        FinanceVendor vendor = null;
        String normalizedCompanyId = trimToNull(paymentCompanyId);
        if (normalizedCompanyId != null) {
            vendor = financeVendorMapper.selectOne(
                    Wrappers.<FinanceVendor>lambdaQuery()
                            .eq(FinanceVendor::getCVenCode, vendorCode)
                            .eq(FinanceVendor::getCompanyId, normalizedCompanyId)
                            .last("limit 1")
            );
        }
        if (vendor == null) {
            vendor = financeVendorMapper.selectOne(
                    Wrappers.<FinanceVendor>lambdaQuery()
                            .eq(FinanceVendor::getCVenCode, vendorCode)
                            .last("limit 1")
            );
        }
        if (vendor == null) {
            return emptyPaymentReceiverInfo();
        }
        return new PaymentReceiverInfo(
                firstNonBlank(vendor.getCVenName(), vendor.getCVenAbbName(), vendorCode),
                trimToNull(vendor.getCVenAccount()),
                firstNonBlank(vendor.getReceiptBranchName(), vendor.getCVenBank()),
                firstNonBlank(vendor.getReceiptAccountName(), vendor.getCVenName()),
                trimToNull(vendor.getReceiptBranchCode()),
                trimToNull(vendor.getReceiptBankProvince()),
                trimToNull(vendor.getReceiptBankCity())
        );
    }

    private PaymentReceiverInfo resolveUserReceiverInfo(String value) {
        String rawId = trimToNull(value);
        if (rawId != null && rawId.startsWith("USER_ACCOUNT:")) {
            rawId = trimToNull(rawId.substring("USER_ACCOUNT:".length()));
        }
        Long accountId = toLong(rawId);
        if (accountId == null) {
            return emptyPaymentReceiverInfo();
        }
        UserBankAccount account = userBankAccountMapper.selectById(accountId);
        if (account == null) {
            return emptyPaymentReceiverInfo();
        }
        return new PaymentReceiverInfo(
                trimToNull(account.getAccountName()),
                trimToNull(account.getAccountNo()),
                firstNonBlank(account.getBranchName(), account.getBankName()),
                trimToNull(account.getAccountName()),
                trimToNull(account.getBranchCode()),
                trimToNull(account.getProvince()),
                trimToNull(account.getCity())
        );
    }

    private BigDecimal resolveActualPaymentAmount(List<ProcessDocumentExpenseDetail> expenseDetails) {
        if (expenseDetails == null || expenseDetails.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return expenseDetails.stream()
                .map(ProcessDocumentExpenseDetail::getActualPaymentAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String resolveBankPushSummary(ProcessDocumentInstance instance) {
        if (instance == null) {
            return null;
        }
        Object value = extractFirstBusinessComponentRawValue(
                readSchema(instance.getFormSchemaSnapshotJson()),
                readMap(instance.getFormDataJson()),
                BANK_PUSH_SUMMARY_COMPONENT_CODE
        );
        return trimObjectToNull(value);
    }
}
