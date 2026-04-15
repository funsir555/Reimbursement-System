// 业务域：首页看板与当前用户
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 MvpController 和首页页面请求，下游会继续协调 用户信息、待办汇总和发票等首页数据。
// 风险提醒：改坏后最容易影响 首页统计、个人信息与待办展示。

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

/**
 * MvpInvoiceDomainSupport：领域规则支撑类。
 * 承接 发票的核心业务规则。
 * 改这里时，要特别关注 首页统计、个人信息与待办展示是否会被一起带坏。
 */
public class MvpInvoiceDomainSupport extends AbstractMvpDomainSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public MvpInvoiceDomainSupport(
            UserService userService,
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            ExpenseDocumentService expenseDocumentService
    ) {
        super(userService, asyncTaskRecordMapper, expenseDocumentService);
    }

    /**
     * 查询发票列表。
     */
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
