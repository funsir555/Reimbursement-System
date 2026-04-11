package com.finex.auth.service.impl.mvp;

import com.finex.auth.dto.DashboardVO;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.dto.UserProfileVO;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MvpDashboardDomainSupportTest {

    @Mock
    private UserService userService;
    @Mock
    private AsyncTaskRecordMapper asyncTaskRecordMapper;
    @Mock
    private ExpenseDocumentService expenseDocumentService;
    @Mock
    private MvpCurrentUserDomainSupport mvpCurrentUserDomainSupport;

    private MvpDashboardDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new MvpDashboardDomainSupport(
                userService,
                asyncTaskRecordMapper,
                expenseDocumentService,
                mvpCurrentUserDomainSupport
        );
    }

    @Test
    void getDashboardAggregatesCountsAndRecentReports() {
        UserProfileVO profile = new UserProfileVO();
        profile.setUserId(6L);
        profile.setName("Alice");

        ExpenseSummaryVO reportOne = new ExpenseSummaryVO();
        reportOne.setDocumentCode("DOC-1");
        reportOne.setTemplateType("report");
        ExpenseSummaryVO reportTwo = new ExpenseSummaryVO();
        reportTwo.setDocumentCode("DOC-2");
        reportTwo.setTemplateType("REPORT");
        ExpenseSummaryVO loan = new ExpenseSummaryVO();
        loan.setDocumentCode("DOC-3");
        loan.setTemplateType("loan");

        when(mvpCurrentUserDomainSupport.getCurrentUser(6L)).thenReturn(profile);
        when(expenseDocumentService.listExpenseSummaries(6L)).thenReturn(List.of(reportOne, reportTwo, loan));
        when(expenseDocumentService.listPendingApprovals(6L)).thenReturn(
                List.of(new ExpenseApprovalPendingItemVO(), new ExpenseApprovalPendingItemVO())
        );
        when(expenseDocumentService.listOutstandingDocuments(6L, "LOAN")).thenReturn(List.of(new ExpenseSummaryVO()));
        when(expenseDocumentService.listOutstandingDocuments(6L, "PREPAY_REPORT")).thenReturn(List.of(new ExpenseSummaryVO(), new ExpenseSummaryVO()));

        DashboardVO result = support.getDashboard(6L);

        assertEquals(6L, result.getUser().getUserId());
        assertEquals(2, result.getPendingApprovalCount());
        assertEquals(1, result.getPendingRepaymentCount());
        assertEquals(2, result.getPendingPrepayWriteOffCount());
        assertEquals(List.of("DOC-1", "DOC-2"), result.getRecentExpenses().stream().map(ExpenseSummaryVO::getDocumentCode).toList());
    }
}
