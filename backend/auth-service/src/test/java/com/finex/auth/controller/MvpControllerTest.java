package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.DashboardVO;
import com.finex.auth.dto.DashboardWriteOffBindingDTO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.dto.UserProfileVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.MvpDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MvpControllerTest {

    @Mock
    private MvpDataService mvpDataService;

    @Mock
    private ExpenseDocumentService expenseDocumentService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MvpController(mvpDataService, expenseDocumentService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void dashboardReturnsAggregatedCounts() throws Exception {
        DashboardVO dashboard = new DashboardVO();
        UserProfileVO user = new UserProfileVO();
        user.setUserId(1L);
        user.setName("张三");
        dashboard.setUser(user);
        dashboard.setPendingApprovalCount(3);
        dashboard.setPendingRepaymentCount(2);
        dashboard.setPendingPrepayWriteOffCount(1);

        doNothing().when(accessControlService).requirePermission(1L, "dashboard:view");
        when(mvpDataService.getDashboard(1L)).thenReturn(dashboard);

        mockMvc.perform(get("/auth/dashboard").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pendingApprovalCount").value(3))
                .andExpect(jsonPath("$.data.pendingRepaymentCount").value(2))
                .andExpect(jsonPath("$.data.pendingPrepayWriteOffCount").value(1));

        verify(mvpDataService).getDashboard(1L);
    }

    @Test
    void outstandingDocumentsEndpointDelegatesToExpenseService() throws Exception {
        ExpenseSummaryVO summary = new ExpenseSummaryVO();
        summary.setDocumentCode("DOC-LOAN-001");
        summary.setOutstandingAmount(BigDecimal.valueOf(88));

        doNothing().when(accessControlService).requirePermission(1L, "dashboard:view");
        when(expenseDocumentService.listOutstandingDocuments(1L, "LOAN")).thenReturn(List.of(summary));

        mockMvc.perform(get("/auth/dashboard/outstanding-documents")
                        .param("kind", "LOAN")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].documentCode").value("DOC-LOAN-001"))
                .andExpect(jsonPath("$.data[0].outstandingAmount").value("88.00"));

        verify(expenseDocumentService).listOutstandingDocuments(1L, "LOAN");
    }

    @Test
    void bindWriteoffEndpointValidatesPayloadAndReturnsSuccess() throws Exception {
        DashboardWriteOffBindingDTO dto = new DashboardWriteOffBindingDTO();
        dto.setTargetDocumentCode("DOC-LOAN-001");
        dto.setSourceReportDocumentCode("DOC-REPORT-001");

        doNothing().when(accessControlService).requirePermission(1L, "dashboard:view");
        when(expenseDocumentService.bindDashboardWriteOff(1L, "DOC-LOAN-001", "DOC-REPORT-001")).thenReturn(true);

        mockMvc.perform(post("/auth/dashboard/writeoff-bindings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        verify(expenseDocumentService).bindDashboardWriteOff(1L, "DOC-LOAN-001", "DOC-REPORT-001");
    }

    @Test
    void writeoffPickerEndpointDelegatesToExpenseService() throws Exception {
        doNothing().when(accessControlService).requirePermission(1L, "dashboard:view");
        when(expenseDocumentService.getDashboardWriteOffSourceReportPicker(1L, "DOC-LOAN-001", "差旅", 1, 10))
                .thenReturn(new com.finex.auth.dto.ExpenseDocumentPickerVO());

        mockMvc.perform(get("/auth/dashboard/writeoff-report-picker")
                        .param("targetDocumentCode", "DOC-LOAN-001")
                        .param("keyword", "差旅")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(expenseDocumentService).getDashboardWriteOffSourceReportPicker(1L, "DOC-LOAN-001", "差旅", 1, 10);
    }
}
