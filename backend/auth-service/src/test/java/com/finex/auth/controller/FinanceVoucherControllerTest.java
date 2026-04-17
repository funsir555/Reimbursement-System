package com.finex.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceVoucherActionResultVO;
import com.finex.auth.dto.FinanceVoucherBatchActionDTO;
import com.finex.auth.dto.FinanceVoucherBatchActionResultVO;
import com.finex.auth.dto.FinanceVoucherDetailVO;
import com.finex.auth.dto.FinanceVoucherEntryDTO;
import com.finex.auth.dto.FinanceVoucherMetaVO;
import com.finex.auth.dto.FinanceVoucherPageVO;
import com.finex.auth.dto.FinanceVoucherQueryDTO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;
import com.finex.auth.dto.FinanceVoucherSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceVoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static final String COMPANY_ID = "COMP-001";
    private static final String VOUCHER_TYPE = "\u8bb0";
    private static final String VOUCHER_NO = "COMP-001~3~\u8bb0~8";
    private static final String DISPLAY_VOUCHER_NO = "\u8bb0-0008";
    private static final String DIGEST_TOO_LONG = "\u5206\u5f55\u6458\u8981\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 255 \u4e2a\u5b57\u7b26";
    private static final String UPDATE_SUCCESS = "\u51ed\u8bc1\u4fee\u6539\u6210\u529f";

    @Mock
    private FinanceVoucherService financeVoucherService;

    @Mock
    private AccessControlService accessControlService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceVoucherController(financeVoucherService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .build();
    }

    @Test
    void metaReturnsVoucherMetaForCurrentUser() throws Exception {
        FinanceVoucherMetaVO meta = new FinanceVoucherMetaVO();
        meta.setDefaultCompanyId(COMPANY_ID);
        meta.setDefaultBillDate("2026-03-28");
        meta.setSuggestedVoucherNo(108);

        doNothing().when(accessControlService)
                .requireAnyPermission(
                        1L,
                        "finance:general_ledger:new_voucher:view",
                        "finance:general_ledger:query_voucher:view",
                        "finance:general_ledger:review_voucher:view"
                );
        when(financeVoucherService.getMeta(1L, "alice", COMPANY_ID, "2026-03-28", VOUCHER_TYPE)).thenReturn(meta);

        mockMvc.perform(get("/auth/finance/vouchers/meta")
                        .param("companyId", COMPANY_ID)
                        .param("billDate", "2026-03-28")
                        .param("csign", VOUCHER_TYPE)
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.defaultCompanyId").value(COMPANY_ID))
                .andExpect(jsonPath("$.data.suggestedVoucherNo").value(108));

        verify(accessControlService).requireAnyPermission(
                1L,
                "finance:general_ledger:new_voucher:view",
                "finance:general_ledger:query_voucher:view",
                "finance:general_ledger:review_voucher:view"
        );
        verify(financeVoucherService).getMeta(1L, "alice", COMPANY_ID, "2026-03-28", VOUCHER_TYPE);
    }

    @Test
    void listPassesStatusFilterAndAllowsReviewPermission() throws Exception {
        FinanceVoucherSummaryVO summary = new FinanceVoucherSummaryVO();
        summary.setVoucherNo(VOUCHER_NO);
        summary.setDisplayVoucherNo(DISPLAY_VOUCHER_NO);
        FinanceVoucherPageVO<FinanceVoucherSummaryVO> page = new FinanceVoucherPageVO<>();
        page.setTotal(1);
        page.setPage(1);
        page.setPageSize(20);
        page.setItems(List.of(summary));

        doNothing().when(accessControlService)
                .requireAnyPermission(1L, "finance:general_ledger:query_voucher:view", "finance:general_ledger:review_voucher:view");
        when(financeVoucherService.queryVouchers(any())).thenReturn(page);

        mockMvc.perform(get("/auth/finance/vouchers")
                        .param("companyId", COMPANY_ID)
                        .param("status", "UNPOSTED,REVIEWED,ERROR")
                        .param("page", "1")
                        .param("pageSize", "20")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].displayVoucherNo").value(DISPLAY_VOUCHER_NO));

        ArgumentCaptor<FinanceVoucherQueryDTO> dtoCaptor = ArgumentCaptor.forClass(FinanceVoucherQueryDTO.class);
        verify(financeVoucherService).queryVouchers(dtoCaptor.capture());
        assertEquals(COMPANY_ID, dtoCaptor.getValue().getCompanyId());
        assertEquals("UNPOSTED,REVIEWED,ERROR", dtoCaptor.getValue().getStatus());
    }

    @Test
    void detailAllowsReviewPermission() throws Exception {
        FinanceVoucherDetailVO detail = new FinanceVoucherDetailVO();
        detail.setVoucherNo(VOUCHER_NO);
        detail.setDisplayVoucherNo(DISPLAY_VOUCHER_NO);

        doNothing().when(accessControlService)
                .requireAnyPermission(1L, "finance:general_ledger:query_voucher:view", "finance:general_ledger:review_voucher:view");
        when(financeVoucherService.getDetail(COMPANY_ID, VOUCHER_NO)).thenReturn(detail);

        mockMvc.perform(get("/auth/finance/vouchers/{voucherNo}", VOUCHER_NO)
                        .param("companyId", COMPANY_ID)
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayVoucherNo").value(DISPLAY_VOUCHER_NO));
    }

    @Test
    void exportReturnsAttachment() throws Exception {
        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:query_voucher:export");
        when(financeVoucherService.exportVouchers(any())).thenReturn("a,b\n1,2".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/auth/finance/vouchers/export")
                        .param("companyId", COMPANY_ID)
                        .param("voucherNo", VOUCHER_NO)
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("filename*=")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("%E5%87%AD%E8%AF%81%E6%9F%A5%E8%AF%A2-")));
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
    void createVoucherRejectsOverlongEntryDigest() throws Exception {
        String body = """
                {
                  "companyId": "COMP-001",
                  "iperiod": 3,
                  "csign": "%s",
                  "dbillDate": "2026-03-28",
                  "entries": [
                    { "inid": 1, "cdigest": "%s", "ccode": "5601", "md": "1280.00" },
                    { "inid": 2, "cdigest": "\u529e\u516c\u7528\u54c1", "ccode": "1002", "mc": "1280.00" }
                  ]
                }
                """.formatted(VOUCHER_TYPE, "A".repeat(256));

        mockMvc.perform(post("/auth/finance/vouchers")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(DIGEST_TOO_LONG));

        verifyNoInteractions(financeVoucherService, accessControlService);
    }

    @Test
    void updateVoucherUsesEditPermission() throws Exception {
        FinanceVoucherSaveResultVO result = new FinanceVoucherSaveResultVO();
        result.setVoucherNo(VOUCHER_NO);
        result.setStatus("UNPOSTED");
        result.setInoId(8);

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:query_voucher:edit");
        when(financeVoucherService.updateVoucher(
                eq(COMPANY_ID),
                eq(VOUCHER_NO),
                any(FinanceVoucherSaveDTO.class),
                eq(1L),
                eq("alice")
        )).thenReturn(result);

        FinanceVoucherSaveDTO dto = buildSaveDto();

        mockMvc.perform(put("/auth/finance/vouchers/{voucherNo}", VOUCHER_NO)
                        .param("companyId", COMPANY_ID)
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "alice")
                        .characterEncoding("UTF-8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(UPDATE_SUCCESS))
                .andExpect(jsonPath("$.data.voucherNo").value(VOUCHER_NO));

        verify(accessControlService).requirePermission(1L, "finance:general_ledger:query_voucher:edit");
        ArgumentCaptor<FinanceVoucherSaveDTO> dtoCaptor = ArgumentCaptor.forClass(FinanceVoucherSaveDTO.class);
        verify(financeVoucherService).updateVoucher(
                eq(COMPANY_ID),
                eq(VOUCHER_NO),
                dtoCaptor.capture(),
                eq(1L),
                eq("alice")
        );
        assertEquals("\u529e\u516c\u8d39\u7528", dtoCaptor.getValue().getEntries().get(0).getCdigest());
        assertEquals("\u94f6\u884c\u4ed8\u6b3e", dtoCaptor.getValue().getEntries().get(1).getCdigest());
    }

    @Test
    void reviewVoucherUsesReviewPermissionAndReturnsNextVoucher() throws Exception {
        FinanceVoucherActionResultVO result = new FinanceVoucherActionResultVO();
        result.setAction("REVIEW");
        result.setVoucherNo(VOUCHER_NO);
        result.setStatus("REVIEWED");
        result.setStatusLabel("\u5df2\u5ba1\u6838");
        result.setCheckerName("\u5f20\u4e09");
        result.setNextVoucherNo("COMP-001~3~\u8bb0~9");
        result.setLastVoucherOfMonth(false);

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:review_voucher:review");
        when(financeVoucherService.reviewVoucher(COMPANY_ID, VOUCHER_NO, 1L, "alice")).thenReturn(result);

        mockMvc.perform(post("/auth/finance/vouchers/{voucherNo}/review", VOUCHER_NO)
                        .param("companyId", COMPANY_ID)
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("\u51ed\u8bc1\u5ba1\u6838\u6210\u529f"))
                .andExpect(jsonPath("$.data.status").value("REVIEWED"))
                .andExpect(jsonPath("$.data.checkerName").value("\u5f20\u4e09"))
                .andExpect(jsonPath("$.data.nextVoucherNo").value("COMP-001~3~\u8bb0~9"));
    }

    @Test
    void unreviewVoucherUsesPermission() throws Exception {
        FinanceVoucherActionResultVO result = new FinanceVoucherActionResultVO();
        result.setAction("UNREVIEW");
        result.setVoucherNo(VOUCHER_NO);
        result.setStatus("UNPOSTED");

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:review_voucher:unreview");
        when(financeVoucherService.unreviewVoucher(COMPANY_ID, VOUCHER_NO)).thenReturn(result);

        mockMvc.perform(post("/auth/finance/vouchers/{voucherNo}/unreview", VOUCHER_NO)
                        .param("companyId", COMPANY_ID)
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("\u51ed\u8bc1\u53cd\u5ba1\u6838\u6210\u529f"))
                .andExpect(jsonPath("$.data.status").value("UNPOSTED"));
    }

    @Test
    void markVoucherErrorUsesPermission() throws Exception {
        FinanceVoucherActionResultVO result = new FinanceVoucherActionResultVO();
        result.setAction("MARK_ERROR");
        result.setVoucherNo(VOUCHER_NO);
        result.setStatus("ERROR");
        result.setStatusLabel("\u5df2\u6807\u8bb0\u9519\u8bef");

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:review_voucher:mark_error");
        when(financeVoucherService.markVoucherError(COMPANY_ID, VOUCHER_NO)).thenReturn(result);

        mockMvc.perform(post("/auth/finance/vouchers/{voucherNo}/mark-error", VOUCHER_NO)
                        .param("companyId", COMPANY_ID)
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("\u51ed\u8bc1\u5df2\u6807\u8bb0\u9519\u8bef"))
                .andExpect(jsonPath("$.data.status").value("ERROR"));
    }

    @Test
    void clearVoucherErrorUsesPermission() throws Exception {
        FinanceVoucherActionResultVO result = new FinanceVoucherActionResultVO();
        result.setAction("CLEAR_ERROR");
        result.setVoucherNo(VOUCHER_NO);
        result.setStatus("UNPOSTED");

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:review_voucher:mark_error");
        when(financeVoucherService.clearVoucherError(COMPANY_ID, VOUCHER_NO)).thenReturn(result);

        mockMvc.perform(post("/auth/finance/vouchers/{voucherNo}/clear-error", VOUCHER_NO)
                        .param("companyId", COMPANY_ID)
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("\u51ed\u8bc1\u9519\u8bef\u6807\u8bb0\u5df2\u53d6\u6d88"))
                .andExpect(jsonPath("$.data.status").value("UNPOSTED"));
    }

    @Test
    void batchUpdateVoucherStateRoutesMarkErrorPermissionAndForwardsPayload() throws Exception {
        FinanceVoucherBatchActionDTO dto = new FinanceVoucherBatchActionDTO();
        dto.setCompanyId(COMPANY_ID);
        dto.setAction("MARK_ERROR");
        dto.setVoucherNos(List.of(VOUCHER_NO, "COMP-001~3~\u8bb0~9"));

        FinanceVoucherBatchActionResultVO result = new FinanceVoucherBatchActionResultVO();
        result.setAction("MARK_ERROR");
        result.setSuccessCount(2);
        result.setVoucherNos(dto.getVoucherNos());

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:review_voucher:mark_error");
        when(financeVoucherService.batchUpdateVoucherState(any(FinanceVoucherBatchActionDTO.class), eq(1L), eq("alice")))
                .thenReturn(result);

        mockMvc.perform(post("/auth/finance/vouchers/actions")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("\u51ed\u8bc1\u6279\u91cf\u72b6\u6001\u66f4\u65b0\u6210\u529f"))
                .andExpect(jsonPath("$.data.action").value("MARK_ERROR"))
                .andExpect(jsonPath("$.data.successCount").value(2));

        ArgumentCaptor<FinanceVoucherBatchActionDTO> dtoCaptor = ArgumentCaptor.forClass(FinanceVoucherBatchActionDTO.class);
        verify(financeVoucherService).batchUpdateVoucherState(dtoCaptor.capture(), eq(1L), eq("alice"));
        assertEquals("MARK_ERROR", dtoCaptor.getValue().getAction());
        assertEquals(2, dtoCaptor.getValue().getVoucherNos().size());
    }

    private FinanceVoucherSaveDTO buildSaveDto() {
        FinanceVoucherSaveDTO dto = new FinanceVoucherSaveDTO();
        dto.setCompanyId(COMPANY_ID);
        dto.setIperiod(3);
        dto.setCsign(VOUCHER_TYPE);
        dto.setInoId(8);
        dto.setDbillDate("2026-03-28");

        FinanceVoucherEntryDTO debitEntry = new FinanceVoucherEntryDTO();
        debitEntry.setInid(1);
        debitEntry.setCdigest("\u529e\u516c\u8d39\u7528");
        debitEntry.setCcode("5601");
        debitEntry.setMd(new BigDecimal("1280.00"));

        FinanceVoucherEntryDTO creditEntry = new FinanceVoucherEntryDTO();
        creditEntry.setInid(2);
        creditEntry.setCdigest("\u94f6\u884c\u4ed8\u6b3e");
        creditEntry.setCcode("1002");
        creditEntry.setMc(new BigDecimal("1280.00"));

        dto.setEntries(List.of(debitEntry, creditEntry));
        return dto;
    }
}
