// 业务域：报销单录入、流转与查询
// 文件角色：定时调度类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl;

import com.finex.auth.service.ExpenseDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ExpensePaymentReceiptScheduler：定时调度类。
 * 负责按触发时机执行 报销单付款回执相关后台任务。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpensePaymentReceiptScheduler {

    private final ExpenseDocumentService expenseDocumentService;

    @Scheduled(
            cron = "${finex.expense.payment.receipt-query-cron:0 0 9 * * *}",
            zone = "${finex.expense.payment.receipt-query-zone:Asia/Shanghai}"
    /**
     * 执行回执Polling。
     */
    )
    public void runReceiptPolling() {
        try {
            expenseDocumentService.runBankReceiptPolling();
        } catch (Exception ex) {
            log.warn("银行回单定时查询失败: {}", ex.getMessage());
        }
    }
}
