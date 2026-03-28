package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceVoucherMetaVO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceVoucherService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinanceVoucherControllerTest {

    @Mock
    private FinanceVoucherService financeVoucherService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceVoucherController(financeVoucherService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void metaReturnsVoucherMetaForCurrentUser() throws Exception {
        FinanceVoucherMetaVO meta = new FinanceVoucherMetaVO();
        meta.setDefaultCompanyId("COMP-001");
        meta.setDefaultBillDate("2026-03-28");
        meta.setSuggestedVoucherNo(108);

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:new_voucher:view");
        when(financeVoucherService.getMeta(1L, "alice", "COMP-001", "2026-03-28", "记")).thenReturn(meta);

        mockMvc.perform(get("/auth/finance/vouchers/meta")
                        .param("companyId", "COMP-001")
                        .param("billDate", "2026-03-28")
                        .param("csign", "记")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.defaultCompanyId").value("COMP-001"))
                .andExpect(jsonPath("$.data.suggestedVoucherNo").value(108));

        verify(accessControlService).requirePermission(1L, "finance:general_ledger:new_voucher:view");
        verify(financeVoucherService).getMeta(1L, "alice", "COMP-001", "2026-03-28", "记");
    }

    @Test
    void createVoucherValidatesRequestBody() throws Exception {
        mockMvc.perform(post("/auth/finance/vouchers")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": "",
                                  "csign": "",
                                  "dbillDate": "",
                                  "entries": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(financeVoucherService, accessControlService);
    }

    @Test
    void createVoucherUsesFallbackUsernameWhenRequestAttributeMissing() throws Exception {
        FinanceVoucherSaveResultVO result = new FinanceVoucherSaveResultVO();
        result.setVoucherNo("记-0008");
        result.setStatus("DRAFT");

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:new_voucher:create");
        when(financeVoucherService.saveVoucher(any(FinanceVoucherSaveDTO.class), eq(1L), eq("财务制单员"))).thenReturn(result);

        mockMvc.perform(post("/auth/finance/vouchers")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": "COMP-001",
                                  "iperiod": 3,
                                  "csign": "记",
                                  "dbillDate": "2026-03-28",
                                  "entries": [
                                    {
                                      "inid": 1,
                                      "cdigest": "办公用品报销",
                                      "ccode": "5601",
                                      "md": 1280
                                    },
                                    {
                                      "inid": 2,
                                      "cdigest": "支付办公用品报销",
                                      "ccode": "1002",
                                      "mc": 1280
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("凭证保存成功"))
                .andExpect(jsonPath("$.data.voucherNo").value("记-0008"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(accessControlService).requirePermission(1L, "finance:general_ledger:new_voucher:create");
        verify(financeVoucherService).saveVoucher(any(FinanceVoucherSaveDTO.class), eq(1L), eq("财务制单员"));
    }
}
