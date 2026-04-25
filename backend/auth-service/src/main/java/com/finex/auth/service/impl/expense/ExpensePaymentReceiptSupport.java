package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.entity.PmBankPaymentRecord;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.SystemCompanyBankAccount;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class ExpensePaymentReceiptSupport extends AbstractExpensePaymentSupport {

    private final ExpensePaymentRecordSupport recordSupport;
    private final ExpensePaymentExecutionSupport executionSupport;

    ExpensePaymentReceiptSupport(
            ExpensePaymentSupportContext context,
            ExpensePaymentRecordSupport recordSupport,
            ExpensePaymentExecutionSupport executionSupport
    ) {
        super(context);
        this.recordSupport = recordSupport;
        this.executionSupport = executionSupport;
    }

    ExpenseDocumentDetailVO handleCmbCloudCallback(ExpenseBankCallbackDTO dto) {
        PmBankPaymentRecord record = recordSupport.requireBankPaymentRecordForCallback(dto);
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
            record.setLastErrorMessage(firstNonBlank(trimToNull(dto.getResultMessage()), "银行回调返回失败"));
            pmBankPaymentRecordMapper.updateById(record);
            throw new IllegalStateException(record.getLastErrorMessage());
        }

        ProcessDocumentTask task = processDocumentTaskMapper.selectById(record.getTaskId());
        if (task == null) {
            throw new IllegalStateException("付款任务不存在");
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
        return executionSupport.completePaymentTaskInternal(
                null,
                SYSTEM_OPERATOR,
                task,
                instance,
                "银行回调确认已支付",
                false,
                paidAt
        );
    }

    void runBankReceiptPolling() {
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
                    "message", "银行尚未返回支付成功结果"
            )));
            recordSupport.saveBankPaymentRecord(record);
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
        recordSupport.saveBankPaymentRecord(record);

        instance.setStatus(DOCUMENT_STATUS_PAYMENT_FINISHED);
        instance.setFinishedAt(now);
        instance.setUpdatedAt(now);
        processDocumentInstanceMapper.updateById(instance);
    }

    private String buildReceiptContent(
            ProcessDocumentInstance instance,
            PmBankPaymentRecord record,
            SystemCompanyBankAccount account
    ) {
        List<String> lines = new ArrayList<>();
        lines.add("招商银行云直连回单");
        lines.add("单据编号: " + defaultText(instance.getDocumentCode(), "-"));
        lines.add("单据名称: " + defaultText(instance.getDocumentTitle(), "-"));
        lines.add("付款账号: " + defaultText(buildCompanyBankAccountName(account), "-"));
        lines.add("银行订单号: " + defaultText(trimToNull(record.getBankOrderNo()), "-"));
        lines.add("银行流水号: " + defaultText(trimToNull(record.getBankFlowNo()), "-"));
        lines.add("支付时间: " + defaultText(formatTime(record.getPaidAt()), "-"));
        lines.add("回单生成时间: " + formatTime(LocalDateTime.now()));
        return String.join(System.lineSeparator(), lines);
    }

    private String buildReceiptFileName(String documentCode) {
        return defaultText(documentCode, "document") + "-银行回单.txt";
    }

    private void verifyCmbCallback(ExpenseBankCallbackDTO dto, SystemCompanyBankAccount account) {
        if (account == null) {
            throw new IllegalStateException("银行回调未绑定公司账户");
        }
        String expectedSecret = trimToNull(readBankLinkExt(account).get("callbackSecret"));
        if (expectedSecret != null && !Objects.equals(expectedSecret, trimToNull(dto.getCallbackSecret()))) {
            throw new IllegalArgumentException("银行回调验签失败");
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
}
