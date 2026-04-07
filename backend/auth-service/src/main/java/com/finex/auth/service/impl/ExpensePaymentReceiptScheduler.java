package com.finex.auth.service.impl;

import com.finex.auth.service.ExpenseDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpensePaymentReceiptScheduler {

    private final ExpenseDocumentService expenseDocumentService;

    @Scheduled(
            cron = "${finex.expense.payment.receipt-query-cron:0 0 9 * * *}",
            zone = "${finex.expense.payment.receipt-query-zone:Asia/Shanghai}"
    )
    public void runReceiptPolling() {
        try {
            expenseDocumentService.runBankReceiptPolling();
        } catch (Exception ex) {
            log.warn("银行回单定时查询失败: {}", ex.getMessage());
        }
    }
}
