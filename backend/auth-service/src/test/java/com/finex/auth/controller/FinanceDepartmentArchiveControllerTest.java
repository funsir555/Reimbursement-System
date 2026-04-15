package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.DepartmentTreeNodeVO;
import com.finex.auth.dto.FinanceDepartmentArchiveMetaVO;
import com.finex.auth.dto.FinanceDepartmentArchiveOptionVO;
import com.finex.auth.dto.FinanceDepartmentVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceDepartmentArchiveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinanceDepartmentArchiveControllerTest {

    @Mock
    private FinanceDepartmentArchiveService financeDepartmentArchiveService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceDepartmentArchiveController(financeDepartmentArchiveService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void metaRequiresPermissionAndReturnsPayload() throws Exception {
        FinanceDepartmentArchiveOptionVO statusOption = new FinanceDepartmentArchiveOptionVO();
        statusOption.setValue("1");
        statusOption.setLabel("启用");

        DepartmentTreeNodeVO department = new DepartmentTreeNodeVO();
        department.setId(10L);
        department.setDeptName("财务部");

        FinanceDepartmentArchiveMetaVO meta = new FinanceDepartmentArchiveMetaVO();
        meta.setDepartments(List.of(department));
        meta.setStatusOptions(List.of(statusOption));

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:departments:view");
        when(financeDepartmentArchiveService.getMeta()).thenReturn(meta);

        mockMvc.perform(get("/auth/finance/archives/departments/meta")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.departments[0].deptName").value("财务部"))
                .andExpect(jsonPath("$.data.statusOptions[0].value").value("1"));

        verify(accessControlService).requirePermission(1L, "finance:archives:departments:view");
        verify(financeDepartmentArchiveService).getMeta();
    }

    @Test
    void queryForwardsFilters() throws Exception {
        FinanceDepartmentVO department = new FinanceDepartmentVO();
        department.setId(11L);
        department.setDeptCode("D001");
        department.setDeptName("费用管理部");
        department.setParentId(10L);
        department.setParentDeptName("财务中心");
        department.setStatus(1);

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:departments:view");
        when(financeDepartmentArchiveService.queryDepartments(any())).thenReturn(List.of(department));

        mockMvc.perform(post("/auth/finance/archives/departments/query")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"费用\",\"parentId\":10,\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].deptCode").value("D001"))
                .andExpect(jsonPath("$.data[0].parentDeptName").value("财务中心"));

        verify(financeDepartmentArchiveService).queryDepartments(any());
    }

    @Test
    void queryRejectsUnauthorizedUser() throws Exception {
        doThrow(new IllegalArgumentException("没有权限")).when(accessControlService)
                .requirePermission(anyLong(), eq("finance:archives:departments:view"));

        mockMvc.perform(post("/auth/finance/archives/departments/query")
                        .requestAttr("currentUserId", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("没有权限"));
    }
}
