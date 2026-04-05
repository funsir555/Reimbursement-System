package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

import java.util.List;

/**
 * 工作台数据
 */
@Data
public class DashboardVO {

    private UserProfileVO user;

    private Integer pendingApprovalCount;

    private Integer pendingApprovalDelta;

    @MoneyValue
    private BigDecimal monthlyExpenseAmount;

    private Integer monthlyExpenseCount;

    private Integer invoiceCount;

    private Integer monthlyInvoiceCount;

    @MoneyValue
    private BigDecimal budgetRemaining;

    private Integer budgetUsageRate;

    private List<ExpenseSummaryVO> recentExpenses;

    private List<ApprovalSummaryVO> pendingApprovals;

    private List<InvoiceAlertVO> invoiceAlerts;
}
