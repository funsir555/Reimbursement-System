package com.finex.auth.service.impl.mvp;

import com.finex.auth.dto.InvoiceSummaryVO;
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
class MvpInvoiceDomainSupportTest {

    @Mock
    private UserService userService;
    @Mock
    private AsyncTaskRecordMapper asyncTaskRecordMapper;
    @Mock
    private ExpenseDocumentService expenseDocumentService;

    private MvpInvoiceDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new MvpInvoiceDomainSupport(userService, asyncTaskRecordMapper, expenseDocumentService);
    }

    @Test
    void listInvoicesMapsLatestVerifyAndOcrStatus() {
        User user = new User();
        user.setId(4L);
        user.setUsername("alice");
        user.setName("Alice");

        AsyncTaskRecord verifyTask = new AsyncTaskRecord();
        verifyTask.setBusinessKey(AsyncTaskSupport.buildInvoiceBusinessKey("011001900211", "12345678"));
        verifyTask.setStatus(AsyncTaskSupport.TASK_STATUS_RUNNING);

        AsyncTaskRecord ocrTask = new AsyncTaskRecord();
        ocrTask.setBusinessKey(AsyncTaskSupport.buildInvoiceBusinessKey("011001900211", "12345678"));
        ocrTask.setStatus(AsyncTaskSupport.TASK_STATUS_FAILED);

        when(userService.getById(4L)).thenReturn(user);
        when(asyncTaskRecordMapper.selectList(any())).thenReturn(List.of(verifyTask), List.of(ocrTask));

        List<InvoiceSummaryVO> result = support.listInvoices(4L);

        assertEquals(4, result.size());
        assertEquals("Verifying", result.get(0).getStatus());
        assertEquals("OCR failed", result.get(0).getOcrStatus());
        assertEquals("Alice Technology Co.", result.get(0).getSeller());
    }
}
