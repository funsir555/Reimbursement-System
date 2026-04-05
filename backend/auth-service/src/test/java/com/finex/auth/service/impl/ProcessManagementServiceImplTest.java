package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessCenterOverviewVO;
import com.finex.auth.dto.ProcessFormDesignSummaryVO;
import com.finex.auth.dto.ProcessTemplateSaveDTO;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessTemplateCategory;
import com.finex.auth.mapper.CodeSequenceMapper;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessCustomArchiveItemMapper;
import com.finex.auth.mapper.ProcessCustomArchiveRuleMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessTemplateCategoryMapper;
import com.finex.auth.mapper.ProcessTemplateScopeMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.ProcessExpenseDetailDesignService;
import com.finex.auth.service.ProcessFlowDesignService;
import com.finex.auth.service.ProcessFormDesignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessManagementServiceImplTest {

    @Mock
    private ProcessTemplateCategoryMapper categoryMapper;
    @Mock
    private ProcessDocumentTemplateMapper templateMapper;
    @Mock
    private CodeSequenceMapper codeSequenceMapper;
    @Mock
    private ProcessTemplateScopeMapper scopeMapper;
    @Mock
    private ProcessCustomArchiveDesignMapper customArchiveDesignMapper;
    @Mock
    private ProcessCustomArchiveItemMapper customArchiveItemMapper;
    @Mock
    private ProcessCustomArchiveRuleMapper customArchiveRuleMapper;
    @Mock
    private ProcessExpenseTypeMapper processExpenseTypeMapper;
    @Mock
    private SystemDepartmentMapper systemDepartmentMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ProcessFormDesignService processFormDesignService;
    @Mock
    private ProcessExpenseDetailDesignService processExpenseDetailDesignService;
    @Mock
    private ProcessFlowDesignService processFlowDesignService;

    private ProcessManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProcessManagementServiceImpl(
                categoryMapper,
                templateMapper,
                codeSequenceMapper,
                scopeMapper,
                customArchiveDesignMapper,
                customArchiveItemMapper,
                customArchiveRuleMapper,
                processExpenseTypeMapper,
                systemDepartmentMapper,
                userMapper,
                processFormDesignService,
                processExpenseDetailDesignService,
                processFlowDesignService,
                new ObjectMapper()
        );
    }

    @Test
    void buildHighlightsReturnsPromptlyWhenOnlyDefaultHighlightExists() {
        ProcessTemplateSaveDTO dto = new ProcessTemplateSaveDTO();
        dto.setPaymentMode("none");
        dto.setAiAuditMode("disabled");

        @SuppressWarnings("unchecked")
        List<String> highlights = assertTimeoutPreemptively(
                Duration.ofSeconds(1),
                () -> (List<String>) ReflectionTestUtils.invokeMethod(service, "buildHighlights", dto, Map.of())
        );

        assertEquals(List.of("移动端提单", "暂无亮点", "暂无亮点"), highlights);
    }

    @Test
    void buildTemplateCodeUsesSequenceAndSkipsExistingValue() {
        when(codeSequenceMapper.allocateNextTemplateCodeValue(any(), any())).thenReturn(0, 1, 1);
        when(templateMapper.selectMaxTemplateCodeValueByPrefix(any())).thenReturn(0L);
        when(codeSequenceMapper.currentAllocatedValue()).thenReturn(1L, 2L);
        when(templateMapper.selectCount(any())).thenReturn(1L, 0L);

        String templateCode = assertTimeoutPreemptively(
                Duration.ofSeconds(1),
                () -> (String) ReflectionTestUtils.invokeMethod(service, "buildTemplateCode")
        );

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertEquals("FX" + today + "0002", templateCode);
    }

    @Test
    void buildTemplateCodeSeedsSequenceFromExistingTemplateCodes() {
        when(codeSequenceMapper.allocateNextTemplateCodeValue(any(), any())).thenReturn(0, 1);
        when(templateMapper.selectMaxTemplateCodeValueByPrefix(any())).thenReturn(8L);
        when(codeSequenceMapper.currentAllocatedValue()).thenReturn(9L);
        when(templateMapper.selectCount(any())).thenReturn(0L);

        String templateCode = assertTimeoutPreemptively(
                Duration.ofSeconds(1),
                () -> (String) ReflectionTestUtils.invokeMethod(service, "buildTemplateCode")
        );

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertEquals("FX" + today + "0009", templateCode);
        verify(codeSequenceMapper).initializeSequenceIfAbsent(any(), any(), any());
    }

    @Test
    void getOverviewIncludesBoundFlowAndFormMetadata() {
        ProcessTemplateCategory category = new ProcessTemplateCategory();
        category.setCategoryCode("employee-expense");
        category.setCategoryName("员工报销");
        category.setCategoryDescription("差旅与报销模板");
        category.setStatus(1);

        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setId(12L);
        template.setTemplateCode("FX202604020001");
        template.setTemplateName("差旅报销单");
        template.setTemplateType("report");
        template.setTemplateTypeLabel("报销单");
        template.setCategoryCode("employee-expense");
        template.setTemplateDescription("差旅费用报销");
        template.setHighlights("移动端提单|AI审单");
        template.setApprovalFlow("FLOW-001");
        template.setFlowName("差旅审批流程");
        template.setFormDesignCode("FD-001");
        template.setOwnerName("流程管理员");
        template.setEnabled(1);
        template.setUpdatedAt(LocalDateTime.of(2026, 4, 2, 9, 30));

        ProcessFormDesignSummaryVO formDesign = new ProcessFormDesignSummaryVO();
        formDesign.setId(7L);
        formDesign.setFormCode("FD-001");
        formDesign.setFormName("差旅报销表单");

        when(categoryMapper.selectList(any())).thenReturn(List.of(category));
        when(templateMapper.selectList(any())).thenReturn(List.of(template));
        when(processFormDesignService.listFormDesigns(null)).thenReturn(List.of(formDesign));

        ProcessCenterOverviewVO overview = service.getOverview();

        assertNotNull(overview);
        assertEquals(1, overview.getCategories().size());
        assertEquals(1, overview.getCategories().get(0).getTemplates().size());
        assertEquals("FLOW-001", overview.getCategories().get(0).getTemplates().get(0).getFlowCode());
        assertEquals("差旅审批流程", overview.getCategories().get(0).getTemplates().get(0).getFlowName());
        assertEquals("FD-001", overview.getCategories().get(0).getTemplates().get(0).getFormCode());
        assertEquals("差旅报销表单", overview.getCategories().get(0).getTemplates().get(0).getFormName());
    }
    @Test
    void getTemplateTypesIncludesContractType() {
        assertTrue(
                service.getTemplateTypes().stream().anyMatch(item ->
                        "contract".equals(item.getCode())
                                && "\u5408\u540c\u5355".equals(item.getName())
                                && "\u5408\u540c\u7ba1\u7406".equals(item.getSubtitle())
                )
        );
    }

}
