package com.finex.auth.service.impl;

import com.finex.auth.dto.DashboardVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.dto.InvoiceSummaryVO;
import com.finex.auth.dto.UserProfileVO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.UserService;
import com.finex.auth.support.AsyncTaskSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MvpDataServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private AsyncTaskRecordMapper asyncTaskRecordMapper;
    @Mock
    private ExpenseDocumentService expenseDocumentService;

    private MvpDataServiceImpl mvpDataService;

    @BeforeEach
    void setUp() {
        mvpDataService = new MvpDataServiceImpl(userService, asyncTaskRecordMapper, expenseDocumentService);
    }

    @Test
    void getCurrentUserDelegatesThroughFacade() {
        User user = new User();
        user.setId(2L);
        user.setUsername("bob");
        user.setName("Bob");

        when(userService.getById(2L)).thenReturn(user);
        when(userService.getRoleCodes(2L)).thenReturn(List.of("USER"));
        when(userService.getPermissionCodes(2L)).thenReturn(List.of("dashboard:view"));

        UserProfileVO result = mvpDataService.getCurrentUser(2L);

        assertEquals("bob", result.getUsername());
        assertEquals(List.of("USER"), result.getRoles());
    }

    @Test
    void getDashboardDelegatesThroughFacade() {
        User user = new User();
        user.setId(2L);
        user.setUsername("bob");
        user.setName("Bob");

        ExpenseSummaryVO report = new ExpenseSummaryVO();
        report.setDocumentCode("DOC-1");
        report.setTemplateType("report");

        when(userService.getById(2L)).thenReturn(user);
        when(userService.getRoleCodes(2L)).thenReturn(List.of("USER"));
        when(userService.getPermissionCodes(2L)).thenReturn(List.of("dashboard:view"));
        when(expenseDocumentService.listExpenseSummaries(2L)).thenReturn(List.of(report));
        when(expenseDocumentService.listPendingApprovals(2L)).thenReturn(List.of());
        when(expenseDocumentService.listOutstandingDocuments(2L, "LOAN")).thenReturn(List.of());
        when(expenseDocumentService.listOutstandingDocuments(2L, "PREPAY_REPORT")).thenReturn(List.of());

        DashboardVO result = mvpDataService.getDashboard(2L);

        assertEquals("DOC-1", result.getRecentExpenses().get(0).getDocumentCode());
        assertEquals(0, result.getPendingApprovalCount());
    }

    @Test
    void listInvoicesDelegatesThroughFacade() {
        User user = new User();
        user.setId(2L);
        user.setUsername("bob");
        user.setName("Bob");

        AsyncTaskRecord verifyTask = new AsyncTaskRecord();
        verifyTask.setBusinessKey(AsyncTaskSupport.buildInvoiceBusinessKey("011001900211", "12345678"));
        verifyTask.setStatus(AsyncTaskSupport.TASK_STATUS_SUCCESS);

        AsyncTaskRecord ocrTask = new AsyncTaskRecord();
        ocrTask.setBusinessKey(AsyncTaskSupport.buildInvoiceBusinessKey("011001900211", "12345678"));
        ocrTask.setStatus(AsyncTaskSupport.TASK_STATUS_RUNNING);

        when(userService.getById(2L)).thenReturn(user);
        when(asyncTaskRecordMapper.selectList(any())).thenReturn(List.of(verifyTask), List.of(ocrTask));

        List<InvoiceSummaryVO> result = mvpDataService.listInvoices(2L);

        assertEquals("Verified", result.get(0).getStatus());
        assertEquals("Recognizing", result.get(0).getOcrStatus());
    }
}
