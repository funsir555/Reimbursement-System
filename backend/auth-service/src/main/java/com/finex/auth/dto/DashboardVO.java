package com.finex.auth.dto;

import lombok.Data;

import java.util.List;

/**
 * 工作台数据
 */
@Data
public class DashboardVO {

    private UserProfileVO user;

    private Integer pendingApprovalCount;

    private Integer pendingApprovalDelta;

    private Double monthlyExpenseAmount;

    private Integer monthlyExpenseCount;

    private Integer invoiceCount;

    private Integer monthlyInvoiceCount;

    private Double budgetRemaining;

    private Integer budgetUsageRate;

    private List<ExpenseSummaryVO> recentExpenses;

    private List<ApprovalSummaryVO> pendingApprovals;

    private List<InvoiceAlertVO> invoiceAlerts;
}
