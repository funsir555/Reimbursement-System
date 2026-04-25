package com.finex.auth.service.impl.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowSceneSaveDTO;
import com.finex.auth.entity.ProcessFlow;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessFlowMapper;
import com.finex.auth.mapper.ProcessFlowNodeMapper;
import com.finex.auth.mapper.ProcessFlowRouteMapper;
import com.finex.auth.mapper.ProcessFlowSceneMapper;
import com.finex.auth.mapper.ProcessFlowVersionMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessFlowMutationDomainSupportTest {

    @Mock private ProcessFlowMapper processFlowMapper;
    @Mock private ProcessFlowVersionMapper processFlowVersionMapper;
    @Mock private ProcessFlowNodeMapper processFlowNodeMapper;
    @Mock private ProcessFlowRouteMapper processFlowRouteMapper;
    @Mock private ProcessFlowSceneMapper processFlowSceneMapper;
    @Mock private SystemCompanyMapper systemCompanyMapper;
    @Mock private SystemDepartmentMapper systemDepartmentMapper;
    @Mock private UserMapper userMapper;
    @Mock private ProcessExpenseTypeMapper processExpenseTypeMapper;
    @Mock private ProcessCustomArchiveDesignMapper processCustomArchiveDesignMapper;
    @Mock private ProcessDocumentTemplateMapper processDocumentTemplateMapper;

    private ProcessFlowMutationDomainSupport support;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        ProcessFlowStructureSupport structureSupport = new ProcessFlowStructureSupport(
                processFlowMapper,
                processFlowVersionMapper,
                processFlowNodeMapper,
                processFlowRouteMapper,
                processFlowSceneMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper,
                processExpenseTypeMapper,
                processCustomArchiveDesignMapper,
                processDocumentTemplateMapper,
                objectMapper
        );
        ProcessFlowQuerySupport querySupport = new ProcessFlowQuerySupport(
                processFlowMapper,
                processFlowVersionMapper,
                processFlowNodeMapper,
                processFlowRouteMapper,
                processFlowSceneMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper,
                processExpenseTypeMapper,
                processCustomArchiveDesignMapper,
                processDocumentTemplateMapper,
                objectMapper,
                structureSupport
        );
        support = new ProcessFlowMutationDomainSupport(
                processFlowMapper,
                processFlowVersionMapper,
                processFlowNodeMapper,
                processFlowRouteMapper,
                processFlowSceneMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper,
                processExpenseTypeMapper,
                processCustomArchiveDesignMapper,
                processDocumentTemplateMapper,
                objectMapper,
                structureSupport,
                querySupport
        );
    }

    @Test
    void updateFlowStatusRejectsEnableWhenNoPublishedVersion() {
        ProcessFlow flow = new ProcessFlow();
        flow.setId(1L);
        flow.setStatus("DRAFT");
        when(processFlowMapper.selectById(1L)).thenReturn(flow);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> support.updateFlowStatus(1L, "ENABLED"));

        assertEquals("当前流程尚未发布，不能直接启用", error.getMessage());
        verify(processFlowMapper, never()).updateById(any());
    }

    @Test
    void createFlowSceneRequiresSceneName() {
        ProcessFlowSceneSaveDTO dto = new ProcessFlowSceneSaveDTO();

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> support.createFlowScene(dto));

        assertEquals("场景名称不能为空", error.getMessage());
    }
}
