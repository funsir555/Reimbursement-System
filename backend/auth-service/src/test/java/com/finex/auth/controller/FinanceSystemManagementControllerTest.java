package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceAccountSetMetaVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceSystemManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinanceSystemManagementControllerTest {

    @Mock
    private FinanceSystemManagementService financeSystemManagementService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceSystemManagementController(financeSystemManagementService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void metaRequiresPermissionAndReturnsPayload() throws Exception {
        FinanceAccountSetMetaVO meta = new FinanceAccountSetMetaVO();
        meta.setDefaultSubjectCodeScheme("4-2-2-2");

        doNothing().when(accessControlService).requirePermission(1L, "finance:system_management:view");
        when(financeSystemManagementService.getMeta()).thenReturn(meta);

        mockMvc.perform(get("/auth/finance/system-management/meta").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.defaultSubjectCodeScheme").value("4-2-2-2"));

        verify(accessControlService).requirePermission(1L, "finance:system_management:view");
        verify(financeSystemManagementService).getMeta();
    }
}