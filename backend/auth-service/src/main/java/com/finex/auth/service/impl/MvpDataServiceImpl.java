// 业务域：首页看板与当前用户
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 MvpController 和首页页面请求，下游会继续协调 用户信息、待办汇总和发票等首页数据。
// 风险提醒：改坏后最容易影响 首页统计、个人信息与待办展示。

package com.finex.auth.service.impl;

import com.finex.auth.dto.DashboardVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.dto.InvoiceSummaryVO;
import com.finex.auth.dto.UserProfileVO;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.MvpDataService;
import com.finex.auth.service.UserService;
import com.finex.auth.service.impl.mvp.MvpCurrentUserDomainSupport;
import com.finex.auth.service.impl.mvp.MvpDashboardDomainSupport;
import com.finex.auth.service.impl.mvp.MvpInvoiceDomainSupport;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MvpDataServiceImpl：service 入口实现。
 * 接住上层请求，并把 数据相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 首页统计、个人信息与待办展示是否会被一起带坏。
 */
@Service
public class MvpDataServiceImpl implements MvpDataService {

    private final MvpCurrentUserDomainSupport mvpCurrentUserDomainSupport;
    private final MvpDashboardDomainSupport mvpDashboardDomainSupport;
    private final MvpInvoiceDomainSupport mvpInvoiceDomainSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public MvpDataServiceImpl(
            UserService userService,
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            ExpenseDocumentService expenseDocumentService
    ) {
        this.mvpCurrentUserDomainSupport = new MvpCurrentUserDomainSupport(
                userService,
                asyncTaskRecordMapper,
                expenseDocumentService
        );
        this.mvpDashboardDomainSupport = new MvpDashboardDomainSupport(
                userService,
                asyncTaskRecordMapper,
                expenseDocumentService,
                mvpCurrentUserDomainSupport
        );
        this.mvpInvoiceDomainSupport = new MvpInvoiceDomainSupport(
                userService,
                asyncTaskRecordMapper,
                expenseDocumentService
        );
    }

    /**
     * 获取当前用户。
     */
    @Override
    public UserProfileVO getCurrentUser(Long userId) {
        return mvpCurrentUserDomainSupport.getCurrentUser(userId);
    }

    /**
     * 获取首页看板。
     */
    @Override
    public DashboardVO getDashboard(Long userId) {
        return mvpDashboardDomainSupport.getDashboard(userId);
    }

    /**
     * 查询报销单列表。
     */
    @Override
    public List<ExpenseSummaryVO> listExpenses(Long userId) {
        return mvpDashboardDomainSupport.listExpenses(userId);
    }

    /**
     * 查询发票列表。
     */
    @Override
    public List<InvoiceSummaryVO> listInvoices(Long userId) {
        return mvpInvoiceDomainSupport.listInvoices(userId);
    }
}
