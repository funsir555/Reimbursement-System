package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.ArchiveAgentDetailVO;
import com.finex.auth.dto.ArchiveAgentMetaVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ArchiveAgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArchiveAgentControllerTest {

    @Mock
    private ArchiveAgentService archiveAgentService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ArchiveAgentController(archiveAgentService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void archiveStatusRequiresDeletePermission() throws Exception {
        ArchiveAgentDetailVO detail = new ArchiveAgentDetailVO();
        detail.setStatus("ARCHIVED");

        doNothing().when(accessControlService).requirePermission(1L, "agents:delete");
        when(archiveAgentService.updateAgentStatus(1L, 2L, "ARCHIVED")).thenReturn(detail);

        mockMvc.perform(post("/auth/archives/agents/2/toggle-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr("currentUserId", 1L)
                        .content("""
                                {
                                  "status": "ARCHIVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        verify(accessControlService).requirePermission(1L, "agents:delete");
    }

    @Test
    void nonArchiveStatusKeepsEditPermission() throws Exception {
        ArchiveAgentDetailVO detail = new ArchiveAgentDetailVO();
        detail.setStatus("DISABLED");

        doNothing().when(accessControlService).requirePermission(1L, "agents:edit");
        when(archiveAgentService.updateAgentStatus(1L, 2L, "DISABLED")).thenReturn(detail);

        mockMvc.perform(post("/auth/archives/agents/2/toggle-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr("currentUserId", 1L)
                        .content("""
                                {
                                  "status": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        verify(accessControlService).requirePermission(1L, "agents:edit");
        verify(archiveAgentService).updateAgentStatus(eq(1L), eq(2L), eq("DISABLED"));
    }

    @Test
    void metaRequiresViewPermissionAndDelegates() throws Exception {
        ArchiveAgentMetaVO meta = new ArchiveAgentMetaVO();
        meta.setDefaultSystemPrompt("test");

        doNothing().when(accessControlService).requirePermission(1L, "agents:view");
        when(archiveAgentService.getMeta()).thenReturn(meta);

        mockMvc.perform(get("/auth/archives/agents/meta")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.defaultSystemPrompt").value("test"));

        verify(accessControlService).requirePermission(1L, "agents:view");
        verify(archiveAgentService).getMeta();
    }
}
