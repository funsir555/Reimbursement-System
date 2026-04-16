package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.BankAccountVO;
import com.finex.auth.dto.ChangePasswordDTO;
import com.finex.auth.dto.DownloadCenterVO;
import com.finex.auth.dto.DownloadRecordVO;
import com.finex.auth.dto.PersonalCenterVO;
import com.finex.auth.dto.UserProfileVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.UserCenterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.core.io.ByteArrayResource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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
class UserCenterControllerTest {

    @Mock
    private UserCenterService userCenterService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserCenterController(userCenterService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void profileReturnsPersonalCenterData() throws Exception {
        PersonalCenterVO profile = new PersonalCenterVO();
        UserProfileVO user = new UserProfileVO();
        user.setUserId(1L);
        user.setUsername("alice");
        profile.setUser(user);

        when(userCenterService.getPersonalCenter(1L)).thenReturn(profile);
        doNothing().when(accessControlService).requirePermission(1L, "profile:view");

        mockMvc.perform(get("/auth/user-center/profile").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.user.username").value("alice"));

        verify(accessControlService).requirePermission(1L, "profile:view");
        verify(userCenterService).getPersonalCenter(1L);
    }

    @Test
    void downloadsUsesAnyPermissionCheck() throws Exception {
        DownloadCenterVO downloadCenter = new DownloadCenterVO();
        DownloadRecordVO record = new DownloadRecordVO();
        record.setId(100L);
        record.setFileName("invoice-export.xlsx");
        record.setDownloadable(true);
        record.setDownloadUrl("/auth/user-center/downloads/100/content");
        downloadCenter.setHistory(List.of(record));

        when(userCenterService.getDownloadCenter(1L)).thenReturn(downloadCenter);
        doNothing().when(accessControlService).requireAnyPermission(1L, "profile:view", "profile:downloads:view");

        mockMvc.perform(get("/auth/user-center/downloads").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.history[0].fileName").value("invoice-export.xlsx"));

        verify(accessControlService).requireAnyPermission(1L, "profile:view", "profile:downloads:view");
        verify(userCenterService).getDownloadCenter(1L);
    }

    @Test
    void listBankAccountsRequiresProfilePermission() throws Exception {
        BankAccountVO account = new BankAccountVO();
        account.setId(8L);
        account.setAccountName("王五");
        account.setBankName("招商银行");
        account.setStatus(1);
        account.setStatusLabel("启用中");

        when(userCenterService.listBankAccounts(1L)).thenReturn(List.of(account));
        doNothing().when(accessControlService).requirePermission(1L, "profile:view");

        mockMvc.perform(get("/auth/user-center/bank-accounts").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].accountName").value("王五"))
                .andExpect(jsonPath("$.data[0].status").value(1));

        verify(accessControlService).requirePermission(1L, "profile:view");
        verify(userCenterService).listBankAccounts(1L);
    }

    @Test
    void createBankAccountRequiresProfilePermissionAndDelegatesToService() throws Exception {
        BankAccountVO account = new BankAccountVO();
        account.setId(9L);
        account.setAccountName("测试账户");
        account.setBankName("中国银行");
        account.setStatus(1);
        account.setStatusLabel("启用中");

        when(userCenterService.createBankAccount(any(), any())).thenReturn(account);
        doNothing().when(accessControlService).requirePermission(1L, "profile:view");

        mockMvc.perform(post("/auth/user-center/bank-accounts")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountName": "测试账户",
                                  "accountNo": "6222020202020202",
                                  "bankCode": "BOC",
                                  "bankName": "中国银行",
                                  "province": "上海市",
                                  "city": "上海市",
                                  "branchCode": "BOC-SH",
                                  "branchName": "中国银行上海分行",
                                  "status": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(9))
                .andExpect(jsonPath("$.data.accountName").value("测试账户"));

        verify(accessControlService).requirePermission(1L, "profile:view");
        verify(userCenterService).createBankAccount(any(), any());
    }

    @Test
    void updateBankAccountStatusRequiresProfilePermission() throws Exception {
        when(userCenterService.updateBankAccountStatus(1L, 12L, 0)).thenReturn(Boolean.TRUE);
        doNothing().when(accessControlService).requirePermission(1L, "profile:view");

        mockMvc.perform(post("/auth/user-center/bank-accounts/12/status")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        verify(accessControlService).requirePermission(1L, "profile:view");
        verify(userCenterService).updateBankAccountStatus(1L, 12L, 0);
    }

    @Test
    void downloadContentStreamsOwnFile() throws Exception {
        when(userCenterService.loadDownloadContent(1L, 100L)).thenReturn(
                new UserCenterService.DownloadContent(
                        new ByteArrayResource("xlsx".getBytes()),
                        "expense-export.xlsx",
                        4L
                )
        );
        doNothing().when(accessControlService).requireAnyPermission(1L, "profile:view", "profile:downloads:view");

        mockMvc.perform(get("/auth/user-center/downloads/100/content").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("expense-export.xlsx")));

        verify(accessControlService).requireAnyPermission(1L, "profile:view", "profile:downloads:view");
        verify(userCenterService).loadDownloadContent(1L, 100L);
    }

    @Test
    void changePasswordValidatesRequestBody() throws Exception {
        mockMvc.perform(post("/auth/user-center/password")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"","newPassword":"","confirmPassword":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(userCenterService);
    }
}
