package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.ArchiveAgentDetailVO;
import com.finex.auth.dto.ArchiveAgentMetaVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ArchiveAgentService;
import com.finex.auth.agent.workflow.AgentWorkbenchToolTestService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArchiveAgentControllerTest {

    @Mock
    private ArchiveAgentService archiveAgentService;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private AgentWorkbenchToolTestService toolTestService;

    @Mock
    private com.finex.auth.agent.workflow.AgentWorkbenchRunPreflightService runPreflightService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ArchiveAgentController(archiveAgentService, accessControlService,
                        toolTestService, runPreflightService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void archiveStatusRequiresDeletePermission() throws Exception {
        ArchiveAgentDetailVO detail = new ArchiveAgentDetailVO();
        detail.setStatus("ARCHIVED");

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

        verify(accessControlService).requireAnyPermission(1L, "agent:workbench:delete", "agents:delete");
    }

    @Test
    void nonArchiveStatusKeepsEditPermission() throws Exception {
        ArchiveAgentDetailVO detail = new ArchiveAgentDetailVO();
        detail.setStatus("DISABLED");

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

        verify(accessControlService).requireAnyPermission(1L, "agent:workbench:edit", "agents:edit");
        verify(archiveAgentService).updateAgentStatus(eq(1L), eq(2L), eq("DISABLED"));
    }

    @Test
    void metaRequiresViewPermissionAndDelegates() throws Exception {
        ArchiveAgentMetaVO meta = new ArchiveAgentMetaVO();
        meta.setDefaultSystemPrompt("test");

        when(archiveAgentService.getMeta()).thenReturn(meta);

        mockMvc.perform(get("/auth/archives/agents/meta")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.defaultSystemPrompt").value("test"));

        verify(accessControlService).requireAnyPermission(1L, "agent:workbench:view", "agents:view");
        verify(archiveAgentService).getMeta();
    }

    @Test
    void savingToolBindingsRequiresEditPermission() throws Exception {
        mockMvc.perform(put("/auth/agent/workbench/2/tools")
                        .contentType(MediaType.APPLICATION_JSON).requestAttr("currentUserId", 1L)
                        .content("{\"expectedVersionNo\":3,\"toolCodes\":[\"expense.query_my_expenses\"]}"))
                .andExpect(status().isOk());
        verify(accessControlService).requireAnyPermission(1L, "agent:workbench:edit", "agents:edit");
        verify(archiveAgentService).updateTools(eq(1L), eq(2L), eq("system"),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testingToolRequiresRunPermission() throws Exception {
        mockMvc.perform(post("/auth/agent/workbench/2/tools/expense.query_my_expenses/test")
                        .contentType(MediaType.APPLICATION_JSON).requestAttr("currentUserId", 1L)
                        .content("{\"parameters\":{\"scope\":\"VISIBLE\"}}"))
                .andExpect(status().isOk());
        verify(accessControlService).requireAnyPermission(1L, "agent:workbench:run", "agents:run");
        verify(toolTestService).test(eq(1L), eq(2L), eq("expense.query_my_expenses"),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void invalidControlledRunIsNotQueued() throws Exception {
        doThrow(new IllegalArgumentException("节点 tool 缺少账套"))
                .when(runPreflightService).validateSubmission(eq(1L), eq(2L),
                        org.mockito.ArgumentMatchers.any());
        mockMvc.perform(post("/auth/agent/workbench/2/run")
                        .contentType(MediaType.APPLICATION_JSON).requestAttr("currentUserId", 1L)
                        .content("{\"inputPayload\":{\"query\":\"查询序时账\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("缺少账套")));
        verify(archiveAgentService, never()).runAgent(eq(1L), eq(2L),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteRequiresDeletePermissionAndDelegates() throws Exception {
        mockMvc.perform(delete("/auth/agent/workbench/2")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Agent 已删除"));

        verify(accessControlService).requireAnyPermission(1L, "agent:workbench:delete", "agents:delete");
        verify(archiveAgentService).deleteAgent(1L, 2L);
    }

    @Test
    void workbenchRunDetailsRequireLogPermission() throws Exception {
        mockMvc.perform(get("/auth/agent/workbench/runs/13")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk());

        verify(accessControlService).requireAnyPermission(1L,
                "agent:workbench:view_logs", "agents:view_logs");
        verify(archiveAgentService).getRunDetail(1L, 13L);
    }

    @Test
    void workbenchRunDetailsRejectUserWithoutLogPermission() throws Exception {
        doThrow(new SecurityException("无权查看工作台日志"))
                .when(accessControlService).requireAnyPermission(1L,
                        "agent:workbench:view_logs", "agents:view_logs");

        mockMvc.perform(get("/auth/agent/workbench/runs/13")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        verify(archiveAgentService, never()).getRunDetail(1L, 13L);
    }
}
