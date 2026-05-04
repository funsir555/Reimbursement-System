package com.finex.auth.service.impl.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowMetaVO;
import com.finex.auth.entity.ProcessFlow;
import com.finex.auth.entity.SystemCompany;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessFlowMetaSupportTest {

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
    @Mock private ProcessUserGroupResolverSupport userGroupResolverSupport;

    private ProcessFlowMetaSupport support;

    @BeforeEach
    void setUp() {
        lenient().when(userGroupResolverSupport.listSecondLevelGroupOptions()).thenReturn(List.of());
        support = new ProcessFlowMetaSupport(
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
                new ObjectMapper(),
                userGroupResolverSupport
        );
    }

    @Test
    void getFlowMetaIncludesCompanyOptionsAndDefaultOpinions() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setCompanyCode("A01");
        company.setCompanyName("广州远智教育科技有限公司");
        company.setStatus(1);
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(company));
        when(processFlowSceneMapper.selectList(any())).thenReturn(List.of());
        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectList(any())).thenReturn(List.of());
        when(processExpenseTypeMapper.selectList(any())).thenReturn(List.of());
        when(processCustomArchiveDesignMapper.selectList(any())).thenReturn(List.of());

        ProcessFlowMetaVO meta = support.getFlowMeta();

        assertEquals("COMPANY_A", meta.getCompanyOptions().get(0).getValue());
        assertEquals("广州远智教育科技有限公司", meta.getCompanyOptions().get(0).getLabel());
        assertEquals(
                List.of(
                        "MANAGER",
                        "DESIGNATED_MEMBER",
                        "DESIGNATED_USER_GROUP",
                        "MANUAL_SELECT"
                ),
                meta.getApprovalApproverTypeOptions().stream()
                        .map(item -> item.getValue())
                        .toList()
        );
        assertTrue(meta.getBranchConditionFields().stream().anyMatch(item -> "paymentCompanyId".equals(item.getKey())));
        assertTrue(meta.getBranchConditionFields().stream().anyMatch(item -> "undertakeDeptIdWithChildren".equals(item.getKey())));
        assertTrue(meta.getBranchConditionFields().stream().anyMatch(item -> "undertakeDeptIdExact".equals(item.getKey())));
        assertTrue(meta.getDefaultApprovalOpinions().contains("通过"));
    }

    @Test
    void publishedFlowLabelMapIncludesEnabledPublishedFlowsOnly() {
        ProcessFlow enabled = new ProcessFlow();
        enabled.setFlowCode("FLOW-001");
        enabled.setFlowName("审批流 A");
        enabled.setStatus("ENABLED");
        enabled.setCurrentPublishedVersionId(11L);
        when(processFlowMapper.selectList(any())).thenReturn(List.of(enabled));

        Map<String, String> labels = support.publishedFlowLabelMap();

        assertEquals(Map.of("FLOW-001", "审批流 A"), labels);
    }
}
