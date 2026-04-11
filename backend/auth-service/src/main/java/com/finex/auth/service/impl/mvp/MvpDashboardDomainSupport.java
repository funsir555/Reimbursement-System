package com.finex.auth.service.impl.mvp;

import cn.hutool.core.util.StrUtil;
import com.finex.auth.dto.DashboardVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.UserService;

import java.math.BigDecimal;
import java.util.List;

public class MvpDashboardDomainSupport extends AbstractMvpDomainSupport {

    private final MvpCurrentUserDomainSupport mvpCurrentUserDomainSupport;

    public MvpDashboardDomainSupport(
            UserService userService,
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            ExpenseDocumentService expenseDocumentService,
            MvpCurrentUserDomainSupport mvpCurrentUserDomainSupport
    ) {
        super(userService, asyncTaskRecordMapper, expenseDocumentService);
        this.mvpCurrentUserDomainSupport = mvpCurrentUserDomainSupport;
    }

    public DashboardVO getDashboard(Long userId) {
        List<ExpenseSummaryVO> recentExpenses = listExpenses(userId).stream()
                .filter(item -> "report".equalsIgnoreCase(StrUtil.blankToDefault(item.getTemplateType(), "")))
                .limit(6)
                .toList();

        DashboardVO dashboard = new DashboardVO();
        dashboard.setUser(mvpCurrentUserDomainSupport.getCurrentUser(userId));
        dashboard.setPendingApprovalCount(expenseDocumentService().listPendingApprovals(userId).size());
        dashboard.setPendingApprovalDelta(0);
        dashboard.setPendingRepaymentCount(expenseDocumentService().listOutstandingDocuments(userId, "LOAN").size());
        dashboard.setPendingPrepayWriteOffCount(expenseDocumentService().listOutstandingDocuments(userId, "PREPAY_REPORT").size());
        dashboard.setUnusedApplicationCount(0);
        dashboard.setUnpaidContractCount(0);
        dashboard.setMonthlyExpenseAmount(BigDecimal.ZERO);
        dashboard.setMonthlyExpenseCount(0);
        dashboard.setInvoiceCount(0);
        dashboard.setMonthlyInvoiceCount(0);
        dashboard.setBudgetRemaining(BigDecimal.ZERO);
        dashboard.setBudgetUsageRate(0);
        dashboard.setRecentExpenses(recentExpenses);
        dashboard.setPendingApprovals(List.of());
        dashboard.setInvoiceAlerts(List.of());
        return dashboard;
    }

    public List<ExpenseSummaryVO> listExpenses(Long userId) {
        return expenseDocumentService().listExpenseSummaries(userId);
    }
}
