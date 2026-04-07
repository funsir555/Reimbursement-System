package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceContextCompanyOptionVO;
import com.finex.auth.dto.FinanceContextMetaVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinanceContextControllerTest {

    @Mock
    private FinanceContextService financeContextService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceContextController(financeContextService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void metaReturnsFinanceCompanyContext() throws Exception {
        FinanceContextCompanyOptionVO option = new FinanceContextCompanyOptionVO();
        option.setCompanyId("COMPANY_A");
        option.setCompanyCode("COMP_A");
        option.setCompanyName("广州远智教育科技有限公司");
        option.setValue("COMPANY_A");
        option.setLabel("COMP_A - 广州远智教育科技有限公司");

        FinanceContextMetaVO meta = new FinanceContextMetaVO();
        meta.getCompanyOptions().add(option);
        meta.setCurrentUserCompanyId("COMPANY_A");
        meta.setDefaultCompanyId("COMPANY_A");

        when(financeContextService.getMeta(1L)).thenReturn(meta);

        mockMvc.perform(get("/auth/finance/context/meta")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.defaultCompanyId").value("COMPANY_A"))
                .andExpect(jsonPath("$.data.companyOptions[0].companyName").value("广州远智教育科技有限公司"));

        verify(financeContextService).getMeta(1L);
    }
}
