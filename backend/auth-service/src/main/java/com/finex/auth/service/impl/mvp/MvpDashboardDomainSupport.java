// 业务域：首页看板与当前用户
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 MvpController 和首页页面请求，下游会继续协调 用户信息、待办汇总和发票等首页数据。
// 风险提醒：改坏后最容易影响 首页统计、个人信息与待办展示。

package com.finex.auth.service.impl.mvp;

import cn.hutool.core.util.StrUtil;
import com.finex.auth.dto.DashboardVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.UserService;

import java.math.BigDecimal;
import java.util.List;

/**
 * MvpDashboardDomainSupport：领域规则支撑类。
 * 承接 首页看板的核心业务规则。
 * 改这里时，要特别关注 首页统计、个人信息与待办展示是否会被一起带坏。
 */
public class MvpDashboardDomainSupport extends AbstractMvpDomainSupport {

    private final MvpCurrentUserDomainSupport mvpCurrentUserDomainSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public MvpDashboardDomainSupport(
            UserService userService,
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            ExpenseDocumentService expenseDocumentService,
            MvpCurrentUserDomainSupport mvpCurrentUserDomainSupport
    ) {
        super(userService, asyncTaskRecordMapper, expenseDocumentService);
        this.mvpCurrentUserDomainSupport = mvpCurrentUserDomainSupport;
    }

    /**
     * 获取首页看板。
     */
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

    /**
     * 查询报销单列表。
     */
    public List<ExpenseSummaryVO> listExpenses(Long userId) {
        return expenseDocumentService().listExpenseSummaries(userId);
    }
}
