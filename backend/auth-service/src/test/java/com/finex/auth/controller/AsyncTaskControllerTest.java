package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.InvoiceTaskSubmitDTO;
import com.finex.auth.dto.NotificationSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.AsyncTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AsyncTaskControllerTest {

    @Mock
    private AsyncTaskService asyncTaskService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AsyncTaskController(asyncTaskService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void exportInvoicesSubmitsTask() throws Exception {
        AsyncTaskSubmitResultVO result = new AsyncTaskSubmitResultVO();
        result.setTaskNo("TASK-001");
        result.setTaskType("EXPORT");
        result.setStatus("PENDING");

        doNothing().when(accessControlService).requirePermission(1L, "archives:invoices:export");
        when(asyncTaskService.submitInvoiceExport(1L)).thenReturn(result);

        mockMvc.perform(post("/auth/async-tasks/exports/invoices").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskNo").value("TASK-001"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(accessControlService).requirePermission(1L, "archives:invoices:export");
        verify(asyncTaskService).submitInvoiceExport(1L);
    }

    @Test
    void verifyInvoiceValidatesRequestBody() throws Exception {
        mockMvc.perform(post("/auth/async-tasks/invoices/verify")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"","number":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(asyncTaskService);
    }

    @Test
    void notificationSummaryReturnsLatestSummary() throws Exception {
        NotificationSummaryVO summary = new NotificationSummaryVO();
        summary.setUnreadCount(3L);
        summary.setLatestTitle("task finished");

        when(asyncTaskService.getNotificationSummary(1L)).thenReturn(summary);

        mockMvc.perform(get("/auth/async-tasks/notifications/summary").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.unreadCount").value(3))
                .andExpect(jsonPath("$.data.latestTitle").value("task finished"));

        verify(asyncTaskService).getNotificationSummary(1L);
    }
}
