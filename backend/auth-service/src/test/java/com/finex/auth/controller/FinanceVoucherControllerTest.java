package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceVoucherDetailVO;
import com.finex.auth.dto.FinanceVoucherMetaVO;
import com.finex.auth.dto.FinanceVoucherPageVO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;
import com.finex.auth.dto.FinanceVoucherSummaryVO;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

        doNothing().when(accessControlService).requireAnyPermission(1L, "finance:general_ledger:new_voucher:view", "finance:general_ledger:query_voucher:view");
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

        verify(accessControlService).requireAnyPermission(1L, "finance:general_ledger:new_voucher:view", "finance:general_ledger:query_voucher:view");
        verify(financeVoucherService).getMeta(1L, "alice", "COMP-001", "2026-03-28", "记");
    }

    @Test
    void listReturnsVoucherPage() throws Exception {
        FinanceVoucherSummaryVO summary = new FinanceVoucherSummaryVO();
        summary.setVoucherNo("COMP-001~3~记~8");
        summary.setDisplayVoucherNo("记-0008");
        FinanceVoucherPageVO<FinanceVoucherSummaryVO> page = new FinanceVoucherPageVO<>();
        page.setTotal(1);
        page.setPage(1);
        page.setPageSize(20);
        page.setItems(List.of(summary));

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:query_voucher:view");
        when(financeVoucherService.queryVouchers(any())).thenReturn(page);

        mockMvc.perform(get("/auth/finance/vouchers")
                        .param("companyId", "COMP-001")
                        .param("page", "1")
                        .param("pageSize", "20")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].displayVoucherNo").value("记-0008"));
    }

    @Test
    void detailRequiresCompanyContext() throws Exception {
        FinanceVoucherDetailVO detail = new FinanceVoucherDetailVO();
        detail.setVoucherNo("COMP-001~3~记~8");
        detail.setDisplayVoucherNo("记-0008");

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:query_voucher:view");
        when(financeVoucherService.getDetail("COMP-001", "COMP-001~3~记~8")).thenReturn(detail);

        mockMvc.perform(get("/auth/finance/vouchers/COMP-001~3~记~8")
                        .param("companyId", "COMP-001")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayVoucherNo").value("记-0008"));
    }

    @Test
    void exportReturnsAttachment() throws Exception {
        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:query_voucher:export");
        when(financeVoucherService.exportVouchers(any())).thenReturn("a,b\n1,2".getBytes());

        mockMvc.perform(get("/auth/finance/vouchers/export")
                        .param("companyId", "COMP-001")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")));
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
    void updateVoucherUsesEditPermission() throws Exception {
        FinanceVoucherSaveResultVO result = new FinanceVoucherSaveResultVO();
        result.setVoucherNo("COMP-001~3~记~8");
        result.setStatus("UNPOSTED");

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:query_voucher:edit");
        when(financeVoucherService.updateVoucher(eq("COMP-001"), eq("COMP-001~3~记~8"), any(FinanceVoucherSaveDTO.class), eq(1L), eq("alice"))).thenReturn(result);

        mockMvc.perform(put("/auth/finance/vouchers/COMP-001~3~记~8")
                        .param("companyId", "COMP-001")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": "COMP-001",
                                  "iperiod": 3,
                                  "csign": "记",
                                  "inoId": 8,
                                  "dbillDate": "2026-03-28",
                                  "entries": [
                                    { "inid": 1, "cdigest": "办公费用", "ccode": "5601", "md": "1280.00" },
                                    { "inid": 2, "cdigest": "支付办公费用", "ccode": "1002", "mc": "1280.00" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voucherNo").value("COMP-001~3~记~8"));
    }
}
