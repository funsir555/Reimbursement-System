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

@Service
public class MvpDataServiceImpl implements MvpDataService {

    private final MvpCurrentUserDomainSupport mvpCurrentUserDomainSupport;
    private final MvpDashboardDomainSupport mvpDashboardDomainSupport;
    private final MvpInvoiceDomainSupport mvpInvoiceDomainSupport;

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

    @Override
    public UserProfileVO getCurrentUser(Long userId) {
        return mvpCurrentUserDomainSupport.getCurrentUser(userId);
    }

    @Override
    public DashboardVO getDashboard(Long userId) {
        return mvpDashboardDomainSupport.getDashboard(userId);
    }

    @Override
    public List<ExpenseSummaryVO> listExpenses(Long userId) {
        return mvpDashboardDomainSupport.listExpenses(userId);
    }

    @Override
    public List<InvoiceSummaryVO> listInvoices(Long userId) {
        return mvpInvoiceDomainSupport.listInvoices(userId);
    }
}
