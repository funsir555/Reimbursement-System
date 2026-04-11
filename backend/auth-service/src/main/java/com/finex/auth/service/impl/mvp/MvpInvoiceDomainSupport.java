package com.finex.auth.service.impl.mvp;

import com.finex.auth.dto.InvoiceSummaryVO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.UserService;
import com.finex.auth.support.AsyncTaskSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class MvpInvoiceDomainSupport extends AbstractMvpDomainSupport {

    public MvpInvoiceDomainSupport(
            UserService userService,
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            ExpenseDocumentService expenseDocumentService
    ) {
        super(userService, asyncTaskRecordMapper, expenseDocumentService);
    }

    public List<InvoiceSummaryVO> listInvoices(Long userId) {
        var user = requireUser(userId);
        int userFactor = Math.max(1, userId.intValue());
        LocalDate today = LocalDate.now();

        List<InvoiceSummaryVO> invoices = List.of(
                invoice("011001900211", "12345678", "VAT invoice", getDisplayName(user) + " Technology Co.",
                        BigDecimal.valueOf(1600L).add(BigDecimal.valueOf(userFactor).multiply(BigDecimal.valueOf(240L))), today.minusDays(3), "Verified", "Recognized"),
                invoice("031001900211", "87654321", "VAT special invoice", getDisplayName(user) + " Trading Co.",
                        BigDecimal.valueOf(3200L).add(BigDecimal.valueOf(userFactor).multiply(BigDecimal.valueOf(360L))), today.minusDays(5), "Verified", "Recognized"),
                invoice("011001900212", "11112222", "Electronic invoice", getDisplayName(user) + " Service Co.",
                        BigDecimal.valueOf(680L).add(BigDecimal.valueOf(userFactor).multiply(BigDecimal.valueOf(100L))), today.minusDays(7), "Pending verify", "Pending OCR"),
                invoice("031001900213", "33334444", "VAT invoice", getDisplayName(user) + " Digital Co.",
                        BigDecimal.valueOf(420L).add(BigDecimal.valueOf(userFactor).multiply(BigDecimal.valueOf(80L))), today.minusDays(12), "Verify failed", "OCR failed")
        );

        Map<String, AsyncTaskRecord> verifyTasks = latestTaskMap(userId, AsyncTaskSupport.TASK_TYPE_INVOICE_VERIFY);
        Map<String, AsyncTaskRecord> ocrTasks = latestTaskMap(userId, AsyncTaskSupport.TASK_TYPE_INVOICE_OCR);

        invoices.forEach(invoice -> {
            String businessKey = AsyncTaskSupport.buildInvoiceBusinessKey(invoice.getCode(), invoice.getNumber());
            applyVerifyStatus(invoice, verifyTasks.get(businessKey));
            applyOcrStatus(invoice, ocrTasks.get(businessKey));
        });
        return invoices;
    }

    private InvoiceSummaryVO invoice(String code, String number, String type, String seller,
                                     BigDecimal amount, LocalDate date, String status, String ocrStatus) {
        InvoiceSummaryVO summary = new InvoiceSummaryVO();
        summary.setCode(code);
        summary.setNumber(number);
        summary.setType(type);
        summary.setSeller(seller);
        summary.setAmount(amount);
        summary.setDate(date.format(DATE_FORMATTER));
        summary.setStatus(status);
        summary.setOcrStatus(ocrStatus);
        return summary;
    }

    private void applyVerifyStatus(InvoiceSummaryVO invoice, AsyncTaskRecord task) {
        if (task == null) {
            return;
        }
        if (AsyncTaskSupport.isActive(task.getStatus())) {
            invoice.setStatus("Verifying");
            return;
        }
        if (AsyncTaskSupport.TASK_STATUS_SUCCESS.equalsIgnoreCase(task.getStatus())) {
            invoice.setStatus("Verified");
            return;
        }
        invoice.setStatus("Verify failed");
    }

    private void applyOcrStatus(InvoiceSummaryVO invoice, AsyncTaskRecord task) {
        if (task == null) {
            return;
        }
        if (AsyncTaskSupport.isActive(task.getStatus())) {
            invoice.setOcrStatus("Recognizing");
            return;
        }
        if (AsyncTaskSupport.TASK_STATUS_SUCCESS.equalsIgnoreCase(task.getStatus())) {
            invoice.setOcrStatus("Recognized");
            return;
        }
        invoice.setOcrStatus("OCR failed");
    }
}
