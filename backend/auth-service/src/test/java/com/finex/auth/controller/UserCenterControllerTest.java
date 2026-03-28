package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
