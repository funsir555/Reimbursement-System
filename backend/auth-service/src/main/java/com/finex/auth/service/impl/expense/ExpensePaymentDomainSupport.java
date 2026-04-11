package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.dto.ExpenseBankLinkConfigVO;
import com.finex.auth.dto.ExpenseBankLinkSaveDTO;
import com.finex.auth.dto.ExpenseBankLinkSummaryVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import com.finex.auth.entity.PmBankPaymentRecord;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.SystemCompanyBankAccount;
import com.finex.auth.mapper.PmBankPaymentRecordMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.mapper.SystemCompanyBankAccountMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.service.ExpenseAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpensePaymentDomainSupport {

    private static final String BANK_PROVIDER_CMB = "CMB";
    private static final String BANK_CHANNEL_CMB_CLOUD = "CMB_CLOUD";
    private static final String SYSTEM_OPERATOR = "SYSTEM";

    private static final String NODE_TYPE_PAYMENT = "PAYMENT";
    private static final String TASK_STATUS_PENDING = "PENDING";
    private static final String TASK_STATUS_PAUSED = "PAUSED";

    private static final String DOCUMENT_STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String DOCUMENT_STATUS_PAYING = "PAYING";
    private static final String DOCUMENT_STATUS_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    private static final String DOCUMENT_STATUS_PAYMENT_FINISHED = "PAYMENT_FINISHED";
    private static final String DOCUMENT_STATUS_PAYMENT_EXCEPTION = "PAYMENT_EXCEPTION";
    private static final String DOCUMENT_STATUS_APPROVED = "APPROVED";
    private static final String DOCUMENT_STATUS_REJECTED = "REJECTED";
    private static final String DOCUMENT_STATUS_EXCEPTION = "EXCEPTION";

    private static final String RECEIPT_STATUS_PENDING = "PENDING";
    private static final String RECEIPT_STATUS_RECEIVED = "RECEIVED";
    private static final String RECEIPT_STATUS_FAILED = "FAILED";

    private final ExpenseDocumentReadSupport expenseDocumentReadSupport;
    private final ExpenseSummaryAssembler expenseSummaryAssembler;
    private final ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;
    private final ExpenseRelationWriteOffService expenseRelationWriteOffService;
    private final PmBankPaymentRecordMapper pmBankPaymentRecordMapper;
    private final ProcessDocumentTaskMapper processDocumentTaskMapper;
    private final ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    private final SystemCompanyBankAccountMapper systemCompanyBankAccountMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final ExpenseAttachmentService expenseAttachmentService;
    private final ObjectMapper objectMapper;

    public List<ExpensePaymentOrderVO> listPaymentOrders(Long userId, String status) {
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
        Map<String, PmBankPaymentRecord> bankRecordMap = loadLatestBankRecordMap(documentCodes);
        Map<Long, String> companyBankAccountNameMap = loadCompanyBankAccountNameMap(
                bankRecordMap.values().stream()
                        .map(PmBankPaymentRecord::getCompanyBankAccountId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );
        return tasks.stream()
                .map(task -> toPaymentOrder(
                        task,
                        instanceMap.get(task.getDocumentCode()),
                        enrichmentData,
                        bankRecordMap.get(task.getDocumentCode()),
                        companyBankAccountNameMap
                ))
                .filter(Objects::nonNull)
                .toList();
    }

    public List<ExpenseBankLinkSummaryVO> listBankLinks() {
        List<SystemCompanyBankAccount> accounts = systemCompanyBankAccountMapper.selectList(
                Wrappers.<SystemCompanyBankAccount>lambdaQuery()
                        .orderByAsc(SystemCompanyBankAccount::getCompanyId)
                        .orderByDesc(SystemCompanyBankAccount::getDirectConnectEnabled)
                        .orderByAsc(SystemCompanyBankAccount::getId)
        );
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, String> companyNameMap = buildCompanyNameMap(
                accounts.stream()
                        .map(SystemCompanyBankAccount::getCompanyId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );
        Map<Long, PmBankPaymentRecord> latestRecordByAccountId = loadLatestBankRecordByAccountId(
                accounts.stream().map(SystemCompanyBankAccount::getId).collect(Collectors.toSet())
        );
        return accounts.stream()
                .map(account -> toBankLinkSummary(account, companyNameMap.get(account.getCompanyId()), latestRecordByAccountId.get(account.getId())))
                .toList();
    }

    public ExpenseBankLinkConfigVO getBankLink(Long companyBankAccountId) {
        SystemCompanyBankAccount account = requireCompanyBankAccount(companyBankAccountId);
        return toBankLinkConfig(account, findCompanyName(account.getCompanyId()));
    }

    public ExpenseBankLinkConfigVO updateBankLink(Long companyBankAccountId, ExpenseBankLinkSaveDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("invalid-config");
        }
        SystemCompanyBankAccount account = requireCompanyBankAccount(companyBankAccountId);
        String provider = trimToNull(dto.getDirectConnectProvider());
        String channel = trimToNull(dto.getDirectConnectChannel());
        if (!BANK_PROVIDER_CMB.equals(provider) || !BANK_CHANNEL_CMB_CLOUD.equals(channel)) {
            throw new IllegalArgumentException("unsupported-bank-link");
        }
        boolean enabled = Boolean.TRUE.equals(dto.getEnabled());
        if (enabled) {
            requireNotBlank(dto.getOperatorKey(), "operatorKey-required");
            requireNotBlank(dto.getCallbackSecret(), "callbackSecret-required");
        }

        account.setDirectConnectEnabled(enabled ? 1 : 0);
        account.setDirectConnectProvider(provider);
        account.setDirectConnectChannel(channel);
        account.setDirectConnectProtocol(trimToNull(dto.getDirectConnectProtocol()));
        account.setDirectConnectCustomerNo(trimToNull(dto.getDirectConnectCustomerNo()));
        account.setDirectConnectAppId(trimToNull(dto.getDirectConnectAppId()));
        account.setDirectConnectAccountAlias(trimToNull(dto.getDirectConnectAccountAlias()));
        account.setDirectConnectAuthMode(trimToNull(dto.getDirectConnectAuthMode()));
        account.setDirectConnectApiBaseUrl(trimToNull(dto.getDirectConnectApiBaseUrl()));
        account.setDirectConnectCertRef(trimToNull(dto.getDirectConnectCertRef()));
        account.setDirectConnectSecretRef(trimToNull(dto.getDirectConnectSecretRef()));
        account.setDirectConnectSignType(trimToNull(dto.getDirectConnectSignType()));
        account.setDirectConnectEncryptType(trimToNull(dto.getDirectConnectEncryptType()));
        account.setDirectConnectLastSyncAt(LocalDateTime.now());
        account.setDirectConnectLastSyncStatus(enabled ? "ENABLED" : "DISABLED");
        account.setDirectConnectLastErrorMsg(null);
        account.setDirectConnectExtJson(writeJson(Map.of(
                "operatorKey", defaultText(trimToNull(dto.getOperatorKey()), ""),
                "callbackSecret", defaultText(trimToNull(dto.getCallbackSecret()), ""),
                "publicKeyRef", defaultText(trimToNull(dto.getPublicKeyRef()), ""),
                "receiptQueryEnabled", Boolean.TRUE.equals(dto.getReceiptQueryEnabled())
        )));
        systemCompanyBankAccountMapper.updateById(account);

        if (enabled) {
            disableOtherEnabledBankLinks(account);
        }
        return toBankLinkConfig(requireCompanyBankAccount(companyBankAccountId), findCompanyName(account.getCompanyId()));
    }
    public ExpenseDocumentDetailVO handleCmbCloudCallback(ExpenseBankCallbackDTO dto) {
        PmBankPaymentRecord record = requireBankPaymentRecordForCallback(dto);
        SystemCompanyBankAccount account = record.getCompanyBankAccountId() == null
                ? null
                : systemCompanyBankAccountMapper.selectById(record.getCompanyBankAccountId());
        verifyCmbCallback(dto, account);

        LocalDateTime now = LocalDateTime.now();
        record.setCallbackPayloadJson(writeJson(dto == null ? Collections.emptyMap() : dto.getRawPayload()));
        record.setCallbackReceivedAt(now);
        record.setBankOrderNo(firstNonBlank(trimToNull(dto.getBankOrderNo()), record.getBankOrderNo()));
        record.setBankFlowNo(firstNonBlank(trimToNull(dto.getBankFlowNo()), record.getBankFlowNo()));
        record.setPushResultJson(writeJson(Map.of(
                "resultCode", defaultText(trimToNull(dto.getResultCode()), ""),
                "resultMessage", defaultText(trimToNull(dto.getResultMessage()), ""),
                "success", resolveCallbackSuccess(dto)
        )));

        if (!resolveCallbackSuccess(dto)) {
            record.setLastErrorMessage(firstNonBlank(trimToNull(dto.getResultMessage()), "\u94F6\u884C\u56DE\u8C03\u8FD4\u56DE\u5931\u8D25"));
            pmBankPaymentRecordMapper.updateById(record);
            throw new IllegalStateException(record.getLastErrorMessage());
        }

        ProcessDocumentTask task = processDocumentTaskMapper.selectById(record.getTaskId());
        if (task == null) {
            throw new IllegalStateException("\u4ED8\u6B3E\u4EFB\u52A1\u4E0D\u5B58\u5728");
        }
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(record.getDocumentCode());
        LocalDateTime paidAt = parseFlexibleDateTime(dto == null ? null : dto.getPaidAt(), now);
        record.setManualPaid(0);
        record.setPaidAt(paidAt);
        if (trimToNull(record.getReceiptStatus()) == null) {
            record.setReceiptStatus(RECEIPT_STATUS_PENDING);
        }
        record.setLastErrorMessage(null);
        pmBankPaymentRecordMapper.updateById(record);

        String status = trimToNull(instance.getStatus());
        if (DOCUMENT_STATUS_PAYMENT_COMPLETED.equals(status) || DOCUMENT_STATUS_PAYMENT_FINISHED.equals(status)) {
            return expenseDocumentReadSupport.buildDocumentDetail(
                    expenseDocumentReadSupport.requireDocument(instance.getDocumentCode())
            );
        }
        return completePaymentTaskInternal(
                null,
                SYSTEM_OPERATOR,
                task,
                instance,
                "\u94F6\u884C\u56DE\u8C03\u786E\u8BA4\u5DF2\u652F\u4ED8",
                false,
                paidAt
        );
    }

    public void runBankReceiptPolling() {
        List<PmBankPaymentRecord> records = pmBankPaymentRecordMapper.selectList(
                Wrappers.<PmBankPaymentRecord>lambdaQuery()
                        .eq(PmBankPaymentRecord::getManualPaid, 0)
                        .and(wrapper -> wrapper.isNull(PmBankPaymentRecord::getReceiptStatus)
                                .or()
                                .ne(PmBankPaymentRecord::getReceiptStatus, RECEIPT_STATUS_RECEIVED))
                        .orderByAsc(PmBankPaymentRecord::getUpdatedAt, PmBankPaymentRecord::getId)
        );
        if (records.isEmpty()) {
            return;
        }
        for (PmBankPaymentRecord record : records) {
            ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(record.getDocumentCode());
            if (!DOCUMENT_STATUS_PAYMENT_COMPLETED.equals(trimToNull(instance.getStatus()))) {
                continue;
            }
            SystemCompanyBankAccount account = record.getCompanyBankAccountId() == null
                    ? null
                    : systemCompanyBankAccountMapper.selectById(record.getCompanyBankAccountId());
            if (!isReceiptQueryEnabled(account)) {
                continue;
            }
            queryAndAttachBankReceipt(record, instance, account);
        }
    }

    public ExpenseDocumentDetailVO startPaymentTask(Long userId, String username, Long taskId) {
        ProcessDocumentTask task = requireOpenPaymentTask(taskId, userId);
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
        String status = trimToNull(instance.getStatus());
        boolean retrying = DOCUMENT_STATUS_PAYMENT_EXCEPTION.equals(status)
                && expenseWorkflowRuntimeSupport.paymentTaskAllowsRetry(instance, task);
        if (DOCUMENT_STATUS_PENDING_PAYMENT.equals(status) || retrying) {
            return pushPaymentTaskToBank(userId, username, task, instance, retrying);
        }
        throw new IllegalStateException("\u5F53\u524D\u4ED8\u6B3E\u4EFB\u52A1\u65E0\u6CD5\u53D1\u8D77\u652F\u4ED8");
    }

    public ExpenseDocumentDetailVO completePaymentTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        ProcessDocumentTask task = requireOpenPaymentTask(taskId, userId);
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
        String status = trimToNull(instance.getStatus());
        if (!DOCUMENT_STATUS_PAYING.equals(status) && !DOCUMENT_STATUS_PENDING_PAYMENT.equals(status)) {
            throw new IllegalStateException("\u5F53\u524D\u4ED8\u6B3E\u4EFB\u52A1\u4E0D\u5728\u53EF\u5B8C\u6210\u72B6\u6001");
        }
        PmBankPaymentRecord record = findOrCreateBankPaymentRecord(task, instance, findActiveBankAccountForDocument(instance));
        record.setManualPaid(1);
        record.setLastErrorMessage(null);
        if (trimToNull(record.getReceiptStatus()) == null) {
            record.setReceiptStatus(RECEIPT_STATUS_PENDING);
        }
        saveBankPaymentRecord(record);
        return completePaymentTaskInternal(
                userId,
                username,
                task,
                instance,
                trimToNull(dto == null ? null : dto.getComment()),
                true,
                LocalDateTime.now()
        );
    }

    public ExpenseDocumentDetailVO markPaymentTaskException(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        ProcessDocumentTask task = requireOpenPaymentTask(taskId, userId);
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
        String status = trimToNull(instance.getStatus());
        if (!DOCUMENT_STATUS_PENDING_PAYMENT.equals(status)
                && !DOCUMENT_STATUS_PAYING.equals(status)
                && !DOCUMENT_STATUS_PAYMENT_EXCEPTION.equals(status)) {
            throw new IllegalStateException("\u5F53\u524D\u4ED8\u6B3E\u4EFB\u52A1\u4E0D\u5728\u53EF\u6807\u8BB0\u5F02\u5E38\u72B6\u6001");
        }

        String comment = trimToNull(dto == null ? null : dto.getComment());
        boolean allowRetry = expenseWorkflowRuntimeSupport.paymentTaskAllowsRetry(instance, task);
        expenseWorkflowRuntimeSupport.markPaymentException(
                instance,
                task,
                userId,
                username,
                comment,
                allowRetry
        );
        PmBankPaymentRecord record = findLatestBankPaymentRecord(instance.getDocumentCode());
        if (record != null) {
            record.setLastErrorMessage(firstNonBlank(comment, "\u4ED8\u6B3E\u5F02\u5E38"));
            record.setReceiptStatus(RECEIPT_STATUS_FAILED);
            pmBankPaymentRecordMapper.updateById(record);
        }
        return expenseDocumentReadSupport.buildDocumentDetail(
                expenseDocumentReadSupport.requireDocument(instance.getDocumentCode())
        );
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
            Map<Long, String> companyBankAccountNameMap
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
        item.setSubmittedAt(formatTime(instance.getCreatedAt()));
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
        return item;
    }
    private ProcessDocumentTask requireOpenPaymentTask(Long taskId, Long userId) {
        ProcessDocumentTask task = processDocumentTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalStateException("\u4ED8\u6B3E\u4EFB\u52A1\u4E0D\u5B58\u5728");
        }
        if (!Objects.equals(task.getAssigneeUserId(), userId)) {
            throw new IllegalStateException("\u5F53\u524D\u7528\u6237\u65E0\u6CD5\u5904\u7406\u8BE5\u4ED8\u6B3E\u4EFB\u52A1");
        }
        if (!NODE_TYPE_PAYMENT.equals(trimToNull(task.getNodeType()))) {
            throw new IllegalStateException("\u5F53\u524D\u4EFB\u52A1\u4E0D\u662F\u4ED8\u6B3E\u4EFB\u52A1");
        }
        if (!TASK_STATUS_PENDING.equals(task.getStatus()) && !TASK_STATUS_PAUSED.equals(task.getStatus())) {
            throw new IllegalStateException("\u4ED8\u6B3E\u4EFB\u52A1\u5DF2\u88AB\u5904\u7406");
        }
        return task;
    }

    private ExpenseDocumentDetailVO pushPaymentTaskToBank(
            Long userId,
            String username,
            ProcessDocumentTask task,
            ProcessDocumentInstance instance,
            boolean retrying
    ) {
        SystemCompanyBankAccount account = findActiveBankAccountForDocument(instance);
        LocalDateTime now = LocalDateTime.now();
        String pushRequestNo = buildBankPushRequestNo(instance.getDocumentCode());
        PmBankPaymentRecord record = findOrCreateBankPaymentRecord(task, instance, account);
        record.setPushRequestNo(pushRequestNo);
        record.setManualPaid(0);
        record.setReceiptStatus(RECEIPT_STATUS_PENDING);
        record.setPushResultJson(writeJson(Map.of(
                "accepted", true,
                "retry", retrying,
                "message", "\u5DF2\u63A8\u9001\u81F3\u94F6\u884C\u76F4\u8FDE\u901A\u9053"
        )));
        record.setLastErrorMessage(null);
        saveBankPaymentRecord(record);

        expenseWorkflowRuntimeSupport.markPaymentStarted(
                instance,
                task,
                userId,
                username,
                retrying,
                account.getId(),
                buildCompanyBankAccountName(account),
                pushRequestNo
        );

        account.setDirectConnectLastSyncAt(now);
        account.setDirectConnectLastSyncStatus("PUSHED");
        account.setDirectConnectLastErrorMsg(null);
        systemCompanyBankAccountMapper.updateById(account);
        return expenseDocumentReadSupport.buildDocumentDetail(
                expenseDocumentReadSupport.requireDocument(instance.getDocumentCode())
        );
    }

    private ExpenseDocumentDetailVO completePaymentTaskInternal(
            Long userId,
            String username,
            ProcessDocumentTask task,
            ProcessDocumentInstance instance,
            String comment,
            boolean manualPaid,
            LocalDateTime paidAt
    ) {
        PmBankPaymentRecord record = findLatestBankPaymentRecord(instance.getDocumentCode());
        if (record == null) {
            SystemCompanyBankAccount account = findActiveBankAccountForDocument(instance, false);
            record = findOrCreateBankPaymentRecord(task, instance, account);
            if (account != null) {
                record.setCompanyBankAccountId(account.getId());
                record.setBankProvider(BANK_PROVIDER_CMB);
                record.setBankChannel(BANK_CHANNEL_CMB_CLOUD);
            }
        }
        record.setManualPaid(manualPaid ? 1 : 0);
        record.setPaidAt(paidAt == null ? LocalDateTime.now() : paidAt);
        if (trimToNull(record.getReceiptStatus()) == null) {
            record.setReceiptStatus(RECEIPT_STATUS_PENDING);
        }
        record.setLastErrorMessage(null);
        saveBankPaymentRecord(record);

        expenseWorkflowRuntimeSupport.completePaymentRuntime(
                instance,
                task,
                userId,
                username,
                comment,
                manualPaid,
                record.getPaidAt()
        );

        String finalStatus = trimToNull(expenseDocumentReadSupport.requireDocument(instance.getDocumentCode()).getStatus());
        if (isEffectiveApprovedStatus(finalStatus)) {
            expenseRelationWriteOffService.finalizeEffectiveWriteOffs(instance.getDocumentCode());
        }
        return expenseDocumentReadSupport.buildDocumentDetail(
                expenseDocumentReadSupport.requireDocument(instance.getDocumentCode())
        );
    }

    private void queryAndAttachBankReceipt(
            PmBankPaymentRecord record,
            ProcessDocumentInstance instance,
            SystemCompanyBankAccount account
    ) {
        LocalDateTime now = LocalDateTime.now();
        record.setLastReceiptQueryAt(now);
        record.setReceiptQueryCount((record.getReceiptQueryCount() == null ? 0 : record.getReceiptQueryCount()) + 1);
        if (record.getPaidAt() == null && record.getCallbackReceivedAt() == null) {
            record.setReceiptResultJson(writeJson(Map.of(
                    "found", false,
                    "message", "\u94F6\u884C\u5C1A\u672A\u8FD4\u56DE\u652F\u4ED8\u6210\u529F\u7ED3\u679C"
            )));
            saveBankPaymentRecord(record);
            return;
        }

        String fileName = buildReceiptFileName(instance.getDocumentCode());
        String receiptBody = buildReceiptContent(instance, record, account);
        var attachment = expenseAttachmentService.saveGeneratedAttachment(
                fileName,
                "text/plain",
                receiptBody.getBytes(StandardCharsets.UTF_8)
        );
        record.setReceiptAttachmentId(attachment.getAttachmentId());
        record.setReceiptFileName(attachment.getFileName());
        record.setReceiptStatus(RECEIPT_STATUS_RECEIVED);
        record.setReceiptReceivedAt(now);
        record.setReceiptResultJson(writeJson(Map.of(
                "found", true,
                "attachmentId", attachment.getAttachmentId(),
                "fileName", attachment.getFileName()
        )));
        record.setLastErrorMessage(null);
        saveBankPaymentRecord(record);

        instance.setStatus(DOCUMENT_STATUS_PAYMENT_FINISHED);
        instance.setFinishedAt(now);
        instance.setUpdatedAt(now);
        processDocumentInstanceMapper.updateById(instance);
    }

    private String buildReceiptContent(ProcessDocumentInstance instance, PmBankPaymentRecord record, SystemCompanyBankAccount account) {
        List<String> lines = new ArrayList<>();
        lines.add("\u62DB\u5546\u94F6\u884C\u4E91\u76F4\u8FDE\u56DE\u5355");
        lines.add("\u5355\u636E\u7F16\u53F7: " + defaultText(instance.getDocumentCode(), "-"));
        lines.add("\u5355\u636E\u540D\u79F0: " + defaultText(instance.getDocumentTitle(), "-"));
        lines.add("\u4ED8\u6B3E\u8D26\u53F7: " + defaultText(buildCompanyBankAccountName(account), "-"));
        lines.add("\u94F6\u884C\u8BA2\u5355\u53F7: " + defaultText(trimToNull(record.getBankOrderNo()), "-"));
        lines.add("\u94F6\u884C\u6D41\u6C34\u53F7: " + defaultText(trimToNull(record.getBankFlowNo()), "-"));
        lines.add("\u652F\u4ED8\u65F6\u95F4: " + defaultText(formatTime(record.getPaidAt()), "-"));
        lines.add("\u56DE\u5355\u751F\u6210\u65F6\u95F4: " + formatTime(LocalDateTime.now()));
        return String.join(System.lineSeparator(), lines);
    }

    private String buildReceiptFileName(String documentCode) {
        return defaultText(documentCode, "document") + "-\u94F6\u884C\u56DE\u5355.txt";
    }

    private Map<String, PmBankPaymentRecord> loadLatestBankRecordMap(List<String> documentCodes) {
        if (documentCodes == null || documentCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return pmBankPaymentRecordMapper.selectList(
                Wrappers.<PmBankPaymentRecord>lambdaQuery()
                        .in(PmBankPaymentRecord::getDocumentCode, documentCodes)
                        .orderByDesc(PmBankPaymentRecord::getId)
        ).stream().collect(Collectors.toMap(
                PmBankPaymentRecord::getDocumentCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private Map<Long, PmBankPaymentRecord> loadLatestBankRecordByAccountId(Set<Long> companyBankAccountIds) {
        if (companyBankAccountIds == null || companyBankAccountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return pmBankPaymentRecordMapper.selectList(
                Wrappers.<PmBankPaymentRecord>lambdaQuery()
                        .in(PmBankPaymentRecord::getCompanyBankAccountId, companyBankAccountIds)
                        .orderByDesc(PmBankPaymentRecord::getId)
        ).stream().collect(Collectors.toMap(
                PmBankPaymentRecord::getCompanyBankAccountId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }
    private Map<Long, String> loadCompanyBankAccountNameMap(Set<Long> companyBankAccountIds) {
        if (companyBankAccountIds == null || companyBankAccountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return systemCompanyBankAccountMapper.selectBatchIds(companyBankAccountIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        SystemCompanyBankAccount::getId,
                        this::buildCompanyBankAccountName,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private ExpenseBankLinkSummaryVO toBankLinkSummary(SystemCompanyBankAccount account, String companyName, PmBankPaymentRecord latestRecord) {
        ExpenseBankLinkSummaryVO item = new ExpenseBankLinkSummaryVO();
        item.setCompanyBankAccountId(account.getId());
        item.setCompanyId(account.getCompanyId());
        item.setCompanyName(companyName);
        item.setAccountName(account.getAccountName());
        item.setAccountNo(maskAccountNo(account.getAccountNo()));
        item.setBankName(account.getBankName());
        item.setAccountStatus(account.getStatus());
        item.setDirectConnectEnabled(isFlagEnabled(account.getDirectConnectEnabled()));
        item.setDirectConnectProvider(account.getDirectConnectProvider());
        item.setDirectConnectChannel(account.getDirectConnectChannel());
        item.setDirectConnectStatusLabel(resolveBankLinkStatusLabel(account));
        item.setLastDirectConnectStatus(resolveBankLinkSyncStatus(account));
        item.setLastReceiptStatus(resolveReceiptStatusLabel(latestRecord));
        return item;
    }

    private ExpenseBankLinkConfigVO toBankLinkConfig(SystemCompanyBankAccount account, String companyName) {
        Map<String, String> ext = readBankLinkExt(account);
        ExpenseBankLinkConfigVO item = new ExpenseBankLinkConfigVO();
        item.setCompanyBankAccountId(account.getId());
        item.setCompanyId(account.getCompanyId());
        item.setCompanyName(companyName);
        item.setAccountName(account.getAccountName());
        item.setAccountNo(account.getAccountNo());
        item.setBankName(account.getBankName());
        item.setAccountStatus(account.getStatus());
        item.setDirectConnectEnabled(isFlagEnabled(account.getDirectConnectEnabled()));
        item.setDirectConnectProvider(account.getDirectConnectProvider());
        item.setDirectConnectChannel(account.getDirectConnectChannel());
        item.setDirectConnectProtocol(account.getDirectConnectProtocol());
        item.setDirectConnectCustomerNo(account.getDirectConnectCustomerNo());
        item.setDirectConnectAppId(account.getDirectConnectAppId());
        item.setDirectConnectAccountAlias(account.getDirectConnectAccountAlias());
        item.setDirectConnectAuthMode(account.getDirectConnectAuthMode());
        item.setDirectConnectApiBaseUrl(account.getDirectConnectApiBaseUrl());
        item.setDirectConnectCertRef(account.getDirectConnectCertRef());
        item.setDirectConnectSecretRef(account.getDirectConnectSecretRef());
        item.setDirectConnectSignType(account.getDirectConnectSignType());
        item.setDirectConnectEncryptType(account.getDirectConnectEncryptType());
        item.setOperatorKey(ext.getOrDefault("operatorKey", ""));
        item.setCallbackSecret(ext.getOrDefault("callbackSecret", ""));
        item.setPublicKeyRef(ext.getOrDefault("publicKeyRef", ""));
        item.setReceiptQueryEnabled(Boolean.parseBoolean(ext.getOrDefault("receiptQueryEnabled", "false")));
        item.setLastDirectConnectStatus(resolveBankLinkSyncStatus(account));
        item.setLastDirectConnectError(account.getDirectConnectLastErrorMsg());
        return item;
    }

    private PmBankPaymentRecord requireBankPaymentRecordForCallback(ExpenseBankCallbackDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("\u94F6\u884C\u56DE\u8C03\u53C2\u6570\u4E0D\u80FD\u4E3A\u7A7A");
        }
        PmBankPaymentRecord record = null;
        if (trimToNull(dto.getPushRequestNo()) != null) {
            record = pmBankPaymentRecordMapper.selectOne(
                    Wrappers.<PmBankPaymentRecord>lambdaQuery()
                            .eq(PmBankPaymentRecord::getPushRequestNo, dto.getPushRequestNo())
                            .orderByDesc(PmBankPaymentRecord::getId)
                            .last("limit 1")
            );
        }
        if (record == null && dto.getTaskId() != null) {
            record = pmBankPaymentRecordMapper.selectOne(
                    Wrappers.<PmBankPaymentRecord>lambdaQuery()
                            .eq(PmBankPaymentRecord::getTaskId, dto.getTaskId())
                            .orderByDesc(PmBankPaymentRecord::getId)
                            .last("limit 1")
            );
        }
        if (record == null && trimToNull(dto.getDocumentCode()) != null) {
            record = findLatestBankPaymentRecord(dto.getDocumentCode());
        }
        if (record == null) {
            throw new IllegalArgumentException("\u672A\u5339\u914D\u5230\u94F6\u884C\u4ED8\u6B3E\u8BB0\u5F55");
        }
        return record;
    }

    private PmBankPaymentRecord findLatestBankPaymentRecord(String documentCode) {
        if (trimToNull(documentCode) == null) {
            return null;
        }
        return pmBankPaymentRecordMapper.selectOne(
                Wrappers.<PmBankPaymentRecord>lambdaQuery()
                        .eq(PmBankPaymentRecord::getDocumentCode, documentCode)
                        .orderByDesc(PmBankPaymentRecord::getId)
                        .last("limit 1")
        );
    }

    private PmBankPaymentRecord findOrCreateBankPaymentRecord(
            ProcessDocumentTask task,
            ProcessDocumentInstance instance,
            SystemCompanyBankAccount account
    ) {
        PmBankPaymentRecord record = findLatestBankPaymentRecord(instance.getDocumentCode());
        if (record == null) {
            record = new PmBankPaymentRecord();
            record.setTaskId(task.getId());
            record.setDocumentCode(instance.getDocumentCode());
            record.setReceiptQueryCount(0);
        }
        if (account != null) {
            record.setCompanyBankAccountId(account.getId());
            record.setBankProvider(BANK_PROVIDER_CMB);
            record.setBankChannel(BANK_CHANNEL_CMB_CLOUD);
        }
        return record;
    }

    private void saveBankPaymentRecord(PmBankPaymentRecord record) {
        if (record.getId() == null) {
            pmBankPaymentRecordMapper.insert(record);
            return;
        }
        pmBankPaymentRecordMapper.updateById(record);
    }

    private SystemCompanyBankAccount findActiveBankAccountForDocument(ProcessDocumentInstance instance) {
        return findActiveBankAccountForDocument(instance, true);
    }

    private SystemCompanyBankAccount findActiveBankAccountForDocument(ProcessDocumentInstance instance, boolean required) {
        ExpenseSummaryAssembler.SummaryMetadata metadata = expenseSummaryAssembler
                .buildSummaryEnrichmentData(List.of(instance))
                .metadata(instance.getDocumentCode());
        String paymentCompanyId = trimToNull(metadata.paymentCompanyId());
        if (paymentCompanyId == null) {
            if (required) {
                throw new IllegalStateException("\u5355\u636E\u672A\u914D\u7F6E\u4ED8\u6B3E\u516C\u53F8\uFF0C\u65E0\u6CD5\u63A8\u9001\u94F6\u884C");
            }
            return null;
        }
        List<SystemCompanyBankAccount> accounts = systemCompanyBankAccountMapper.selectList(
                Wrappers.<SystemCompanyBankAccount>lambdaQuery()
                        .eq(SystemCompanyBankAccount::getCompanyId, paymentCompanyId)
                        .eq(SystemCompanyBankAccount::getStatus, 1)
                        .eq(SystemCompanyBankAccount::getDirectConnectEnabled, 1)
                        .eq(SystemCompanyBankAccount::getDirectConnectProvider, BANK_PROVIDER_CMB)
                        .eq(SystemCompanyBankAccount::getDirectConnectChannel, BANK_CHANNEL_CMB_CLOUD)
                        .orderByAsc(SystemCompanyBankAccount::getId)
        );
        if (accounts.isEmpty()) {
            if (required) {
                throw new IllegalStateException("\u4ED8\u6B3E\u516C\u53F8\u672A\u542F\u7528\u62DB\u5546\u94F6\u884C\u4E91\u76F4\u8FDE\u8D26\u6237");
            }
            return null;
        }
        if (accounts.size() > 1) {
            throw new IllegalStateException("\u540C\u4E00\u516C\u53F8\u53EA\u80FD\u542F\u7528\u4E00\u4E2A\u62DB\u5546\u94F6\u884C\u4E91\u76F4\u8FDE\u8D26\u6237");
        }
        return accounts.get(0);
    }

    private SystemCompanyBankAccount requireCompanyBankAccount(Long companyBankAccountId) {
        SystemCompanyBankAccount account = systemCompanyBankAccountMapper.selectById(companyBankAccountId);
        if (account == null) {
            throw new IllegalArgumentException("闂佺娴氶崜娆忣嚗閸愵亝瀚婚柨鏃囨閻撴洖鈽夐幘宕囆㈤柣掳鍔戝畷?");
        }
        return account;
    }

    private void disableOtherEnabledBankLinks(SystemCompanyBankAccount currentAccount) {
        List<SystemCompanyBankAccount> companyAccounts = systemCompanyBankAccountMapper.selectList(
                Wrappers.<SystemCompanyBankAccount>lambdaQuery()
                        .eq(SystemCompanyBankAccount::getCompanyId, currentAccount.getCompanyId())
                        .eq(SystemCompanyBankAccount::getDirectConnectEnabled, 1)
                        .eq(SystemCompanyBankAccount::getDirectConnectProvider, BANK_PROVIDER_CMB)
                        .eq(SystemCompanyBankAccount::getDirectConnectChannel, BANK_CHANNEL_CMB_CLOUD)
        );
        for (SystemCompanyBankAccount account : companyAccounts) {
            if (Objects.equals(account.getId(), currentAccount.getId())) {
                continue;
            }
            account.setDirectConnectEnabled(0);
            account.setDirectConnectLastSyncStatus("DISABLED");
            systemCompanyBankAccountMapper.updateById(account);
        }
    }
    private Map<String, String> buildCompanyNameMap(Set<String> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return systemCompanyMapper.selectList(
                Wrappers.<SystemCompany>lambdaQuery()
                        .in(SystemCompany::getCompanyId, companyIds)
        ).stream().collect(Collectors.toMap(
                SystemCompany::getCompanyId,
                item -> defaultText(trimToNull(item.getCompanyName()), item.getCompanyId()),
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private String findCompanyName(String companyId) {
        if (trimToNull(companyId) == null) {
            return "";
        }
        SystemCompany company = systemCompanyMapper.selectOne(
                Wrappers.<SystemCompany>lambdaQuery()
                        .eq(SystemCompany::getCompanyId, companyId)
                        .last("limit 1")
        );
        return company == null ? companyId : defaultText(trimToNull(company.getCompanyName()), companyId);
    }

    private void verifyCmbCallback(ExpenseBankCallbackDTO dto, SystemCompanyBankAccount account) {
        if (account == null) {
            throw new IllegalStateException("\u94F6\u884C\u56DE\u8C03\u672A\u7ED1\u5B9A\u516C\u53F8\u8D26\u6237");
        }
        String expectedSecret = trimToNull(readBankLinkExt(account).get("callbackSecret"));
        if (expectedSecret != null && !Objects.equals(expectedSecret, trimToNull(dto.getCallbackSecret()))) {
            throw new IllegalArgumentException("\u94F6\u884C\u56DE\u8C03\u9A8C\u7B7E\u5931\u8D25");
        }
    }

    private boolean resolveCallbackSuccess(ExpenseBankCallbackDTO dto) {
        if (dto == null) {
            return false;
        }
        if (dto.getSuccess() != null) {
            return dto.getSuccess();
        }
        String resultCode = defaultText(trimToNull(dto.getResultCode()), "");
        return Set.of("SUCCESS", "ACCEPTED", "00", "200").contains(resultCode);
    }

    private String resolveReceiptStatusLabel(PmBankPaymentRecord record) {
        if (record == null) {
            return "闂佸搫鐗滄禍鐐哄极閹捐绠?";
        }
        if (isFlagEnabled(record.getManualPaid()) && trimToNull(record.getReceiptAttachmentId()) == null) {
            return "manual-paid";
        }
        return switch (defaultText(trimToNull(record.getReceiptStatus()), RECEIPT_STATUS_PENDING)) {
            case RECEIPT_STATUS_RECEIVED -> "received";
            case RECEIPT_STATUS_FAILED -> "failed";
            default -> "pending";
        };
    }

    private String resolveBankLinkStatusLabel(SystemCompanyBankAccount account) {
        if (!isFlagEnabled(account.getDirectConnectEnabled())) {
            return "闂佸搫鐗滄禍婊堝箚鎼淬劍鍋?";
        }
        if (!BANK_PROVIDER_CMB.equals(trimToNull(account.getDirectConnectProvider()))
                || !BANK_CHANNEL_CMB_CLOUD.equals(trimToNull(account.getDirectConnectChannel()))) {
            return "闂佸搫鐗滄禍顏堝储閵堝洨纾?";
        }
        return "閻庣懓鎲¤ぐ鍐箚鎼淬劍鍋?";
    }

    private String resolveBankLinkSyncStatus(SystemCompanyBankAccount account) {
        String status = trimToNull(account.getDirectConnectLastSyncStatus());
        return status == null ? "闂佸搫鐗滄禍婵堟暜瑜版帗鐒?" : status;
    }

    private boolean isReceiptQueryEnabled(SystemCompanyBankAccount account) {
        if (account == null) {
            return false;
        }
        return Boolean.parseBoolean(readBankLinkExt(account).getOrDefault("receiptQueryEnabled", "false"));
    }

    private Map<String, String> readBankLinkExt(SystemCompanyBankAccount account) {
        Map<String, Object> ext = readMap(account == null ? null : account.getDirectConnectExtJson());
        Map<String, String> result = new LinkedHashMap<>();
        ext.forEach((key, value) -> result.put(key, value == null ? "" : String.valueOf(value)));
        return result;
    }

    private String buildCompanyBankAccountName(SystemCompanyBankAccount account) {
        if (account == null) {
            return null;
        }
        String tailNo = trimToNull(account.getAccountNo());
        String suffix = tailNo == null || tailNo.length() <= 4 ? tailNo : tailNo.substring(tailNo.length() - 4);
        return account.getAccountName() + (suffix == null ? "" : "\uFF08\u5C3E\u53F7 " + suffix + "\uFF09");
    }

    private String buildBankPushRequestNo(String documentCode) {
        return defaultText(documentCode, "DOC") + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private LocalDateTime parseFlexibleDateTime(String rawValue, LocalDateTime defaultValue) {
        String normalized = trimToNull(rawValue);
        if (normalized == null) {
            return defaultValue;
        }
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (Exception ignored) {
                // try next formatter
            }
        }
        return defaultValue;
    }

    private String normalizePaymentOrderStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return DOCUMENT_STATUS_PENDING_PAYMENT;
        }
        return switch (normalized) {
            case DOCUMENT_STATUS_PENDING_PAYMENT,
                    DOCUMENT_STATUS_PAYING,
                    DOCUMENT_STATUS_PAYMENT_COMPLETED,
                    DOCUMENT_STATUS_PAYMENT_FINISHED,
                    DOCUMENT_STATUS_PAYMENT_EXCEPTION -> normalized;
            default -> DOCUMENT_STATUS_PENDING_PAYMENT;
        };
    }

    private boolean isEffectiveApprovedStatus(String status) {
        String normalized = trimToNull(status);
        return DOCUMENT_STATUS_APPROVED.equals(normalized)
                || DOCUMENT_STATUS_PAYMENT_COMPLETED.equals(normalized)
                || DOCUMENT_STATUS_PAYMENT_FINISHED.equals(normalized);
    }

    private String resolveTemplateTypeLabel(String templateType, String currentLabel) {
        if (trimToNull(currentLabel) != null) {
            return currentLabel;
        }
        return switch (trimToNull(templateType) == null ? "report" : templateType.trim()) {
            case "application" -> "\u7533\u8BF7\u5355";
            case "loan" -> "\u501F\u6B3E\u5355";
            case "contract" -> "\u5408\u540C\u5355";
            default -> "pending";
        };
    }

    private String resolveStatusLabel(String status) {
        return switch (trimToNull(status) == null ? "" : status.trim()) {
            case DOCUMENT_STATUS_PENDING_PAYMENT -> "\u5F85\u652F\u4ED8";
            case DOCUMENT_STATUS_PAYING -> "\u652F\u4ED8\u4E2D";
            case DOCUMENT_STATUS_PAYMENT_COMPLETED -> "\u5DF2\u652F\u4ED8";
            case DOCUMENT_STATUS_PAYMENT_FINISHED -> "\u5DF2\u5B8C\u6210";
            case DOCUMENT_STATUS_PAYMENT_EXCEPTION -> "\u652F\u4ED8\u5F02\u5E38";
            case DOCUMENT_STATUS_APPROVED -> "\u5DF2\u901A\u8FC7";
            case DOCUMENT_STATUS_REJECTED -> "\u5DF2\u9A73\u56DE";
            case "DRAFT" -> "\u8349\u7A3F";
            case DOCUMENT_STATUS_EXCEPTION -> "\u6D41\u7A0B\u5F02\u5E38";
            default -> "pending";
        };
    }
    private String formatTime(LocalDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private Map<String, Object> readMap(String json) {
        if (trimToNull(json) == null) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, LinkedHashMap.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse json map", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize data", ex);
        }
    }

    private void requireNotBlank(String value, String message) {
        if (trimToNull(value) == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private boolean isFlagEnabled(Integer value) {
        return value != null && value == 1;
    }

    private String maskAccountNo(String accountNo) {
        String normalized = trimToNull(accountNo);
        if (normalized == null) {
            return "";
        }
        if (normalized.length() <= 8) {
            return normalized;
        }
        return normalized.substring(0, 4) + " **** " + normalized.substring(normalized.length() - 4);
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

    private String defaultText(String value, String fallback) {
        return trimToNull(value) == null ? fallback : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
