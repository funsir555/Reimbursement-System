package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessCenterOverviewVO;
import com.finex.auth.dto.ProcessExpenseDetailDesignSummaryVO;
import com.finex.auth.dto.ProcessFormDesignSummaryVO;
import com.finex.auth.dto.ProcessTemplateSaveResultVO;
import com.finex.auth.dto.ProcessTemplateSaveDTO;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessCustomArchiveItem;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseType;
import com.finex.auth.entity.ProcessTemplateCategory;
import com.finex.auth.entity.ProcessTemplateScope;
import com.finex.auth.entity.SystemDepartment;
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
import org.apache.ibatis.builder.MapperBuilderAssistant;
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
        initTableInfo(ProcessDocumentTemplate.class);
        initTableInfo(ProcessTemplateCategory.class);
        initTableInfo(ProcessTemplateScope.class);
        initTableInfo(ProcessCustomArchiveDesign.class);
        initTableInfo(ProcessCustomArchiveItem.class);
        initTableInfo(ProcessExpenseType.class);
        initTableInfo(SystemDepartment.class);
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

    private void initTableInfo(Class<?> entityClass) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entityClass);
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

        assertEquals(
                List.of("\u79fb\u52a8\u7aef\u63d0\u5355", "\u6682\u65e0\u4eae\u70b9", "\u6682\u65e0\u4eae\u70b9"),
                highlights
        );
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
    void getOverviewIncludesBoundFlowFormAndExpenseDetailMetadata() {
        ProcessTemplateCategory category = new ProcessTemplateCategory();
        category.setCategoryCode("employee-expense");
        category.setCategoryName("\u5458\u5de5\u62a5\u9500");
        category.setCategoryDescription("\u5dee\u65c5\u4e0e\u62a5\u9500\u6a21\u677f");
        category.setStatus(1);

        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setId(12L);
        template.setTemplateCode("FX202604020001");
        template.setTemplateName("\u5dee\u65c5\u62a5\u9500\u5355");
        template.setTemplateType("report");
        template.setTemplateTypeLabel("\u62a5\u9500\u5355");
        template.setCategoryCode("employee-expense");
        template.setTemplateDescription("\u5dee\u65c5\u8d39\u7528\u62a5\u9500");
        template.setHighlights("\u79fb\u52a8\u7aef\u63d0\u5355|AI\u5ba1\u5355");
        template.setApprovalFlow("FLOW-001");
        template.setFlowName("\u5dee\u65c5\u5ba1\u6279\u6d41\u7a0b");
        template.setFormDesignCode("FD-001");
        template.setExpenseDetailDesignCode("EDD-001");
        template.setOwnerName("\u6d41\u7a0b\u7ba1\u7406\u5458");
        template.setEnabled(1);
        template.setUpdatedAt(LocalDateTime.of(2026, 4, 2, 9, 30));

        ProcessFormDesignSummaryVO formDesign = new ProcessFormDesignSummaryVO();
        formDesign.setId(7L);
        formDesign.setFormCode("FD-001");
        formDesign.setFormName("\u5dee\u65c5\u62a5\u9500\u8868\u5355");

        ProcessExpenseDetailDesignSummaryVO expenseDetailDesign = new ProcessExpenseDetailDesignSummaryVO();
        expenseDetailDesign.setId(9L);
        expenseDetailDesign.setDetailCode("EDD-001");
        expenseDetailDesign.setDetailName("\u8d39\u7528\u660e\u7ec6\u8868\u5355");

        when(categoryMapper.selectList(any())).thenReturn(List.of(category));
        when(templateMapper.selectList(any())).thenReturn(List.of(template));
        when(processFormDesignService.listFormDesigns(null)).thenReturn(List.of(formDesign));
        when(processExpenseDetailDesignService.listExpenseDetailDesigns()).thenReturn(List.of(expenseDetailDesign));

        ProcessCenterOverviewVO overview = service.getOverview();

        assertNotNull(overview);
        assertEquals(1, overview.getCategories().size());
        assertEquals(1, overview.getCategories().get(0).getTemplates().size());
        assertEquals("FLOW-001", overview.getCategories().get(0).getTemplates().get(0).getFlowCode());
        assertEquals("\u5dee\u65c5\u5ba1\u6279\u6d41\u7a0b", overview.getCategories().get(0).getTemplates().get(0).getFlowName());
        assertEquals("FD-001", overview.getCategories().get(0).getTemplates().get(0).getFormCode());
        assertEquals("\u5dee\u65c5\u62a5\u9500\u8868\u5355", overview.getCategories().get(0).getTemplates().get(0).getFormName());
        assertEquals("EDD-001", overview.getCategories().get(0).getTemplates().get(0).getExpenseDetailDesignCode());
        assertEquals("\u8d39\u7528\u660e\u7ec6\u8868\u5355", overview.getCategories().get(0).getTemplates().get(0).getExpenseDetailDesignName());
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

    @Test
    void copyTemplateCreatesDraftDuplicateWithCopiedBindings() {
        ProcessDocumentTemplate source = new ProcessDocumentTemplate();
        source.setId(12L);
        source.setTemplateName("\u5dee\u65c5\u62a5\u9500\u5355");
        source.setTemplateType("report");
        source.setCategoryCode("employee-expense");
        source.setTemplateDescription("\u5dee\u65c5\u8d39\u7528\u62a5\u9500");
        source.setFormDesignCode("FD-001");
        source.setExpenseDetailDesignCode("EDD-001");
        source.setExpenseDetailModeDefault("PREPAY_UNBILLED");
        source.setPrintMode("default-print");
        source.setApprovalFlow("FLOW-001");
        source.setPaymentMode("none");
        source.setAllocationForm("allocation-default");
        source.setAiAuditMode("disabled");
        source.setEnabled(1);
        source.setPublishStatus("ENABLED");
        source.setOwnerName("\u6d41\u7a0b\u7ba1\u7406\u5458");

        when(templateMapper.selectById(12L)).thenReturn(source);
        when(scopeMapper.selectList(any())).thenReturn(List.of());
        when(processFormDesignService.resolveFormDesignCode("FD-001", "report")).thenReturn("FD-001");
        when(processExpenseDetailDesignService.resolveExpenseDetailDesignCode("EDD-001")).thenReturn("EDD-001");
        when(processFlowDesignService.publishedFlowLabelMap()).thenReturn(Map.of("FLOW-001", "\u5dee\u65c5\u5ba1\u6279\u6d41\u7a0b"));
        when(customArchiveDesignMapper.selectList(any())).thenReturn(List.of());
        when(templateMapper.selectList(any())).thenReturn(List.of());
        when(codeSequenceMapper.allocateNextTemplateCodeValue(any(), any())).thenReturn(0, 1);
        when(templateMapper.selectMaxTemplateCodeValueByPrefix(any())).thenReturn(0L);
        when(codeSequenceMapper.currentAllocatedValue()).thenReturn(1L);
        when(templateMapper.selectCount(any())).thenReturn(0L, 0L);
        org.mockito.Mockito.doAnswer(invocation -> {
            ProcessDocumentTemplate inserted = invocation.getArgument(0);
            inserted.setId(88L);
            return 1;
        }).when(templateMapper).insert(any(ProcessDocumentTemplate.class));

        ProcessTemplateSaveResultVO result = service.copyTemplate(12L, "Operator");

        assertNotNull(result);
        assertEquals(88L, result.getId());
        assertEquals("DRAFT", result.getStatus());
        assertEquals("\u5dee\u65c5\u62a5\u9500\u5355 - \u526f\u672c", result.getTemplateName());
    }

    @Test
    void getOverviewCountsEnabledAndDraftTemplatesSeparately() {
        ProcessTemplateCategory category = new ProcessTemplateCategory();
        category.setCategoryCode("employee-expense");
        category.setCategoryName("\u5458\u5de5\u62a5\u9500");
        category.setCategoryDescription("\u5dee\u65c5\u4e0e\u62a5\u9500\u6a21\u677f");
        category.setStatus(1);

        ProcessDocumentTemplate enabledTemplate = new ProcessDocumentTemplate();
        enabledTemplate.setId(12L);
        enabledTemplate.setTemplateCode("FX202604020001");
        enabledTemplate.setTemplateName("\u5dee\u65c5\u62a5\u9500\u5355");
        enabledTemplate.setTemplateType("report");
        enabledTemplate.setTemplateTypeLabel("\u62a5\u9500\u5355");
        enabledTemplate.setCategoryCode("employee-expense");
        enabledTemplate.setTemplateDescription("\u5dee\u65c5\u8d39\u7528\u62a5\u9500");
        enabledTemplate.setApprovalFlow("FLOW-001");
        enabledTemplate.setFlowName("\u5dee\u65c5\u5ba1\u6279\u6d41\u7a0b");
        enabledTemplate.setFormDesignCode("FD-001");
        enabledTemplate.setEnabled(1);
        enabledTemplate.setPublishStatus("ENABLED");
        enabledTemplate.setUpdatedAt(LocalDateTime.of(2026, 4, 2, 9, 30));

        ProcessDocumentTemplate draftTemplate = new ProcessDocumentTemplate();
        draftTemplate.setId(13L);
        draftTemplate.setTemplateCode("FX202604020002");
        draftTemplate.setTemplateName("\u5dee\u65c5\u62a5\u9500\u5355 - \u526f\u672c");
        draftTemplate.setTemplateType("report");
        draftTemplate.setTemplateTypeLabel("\u62a5\u9500\u5355");
        draftTemplate.setCategoryCode("employee-expense");
        draftTemplate.setTemplateDescription("\u5dee\u65c5\u8d39\u7528\u62a5\u9500");
        draftTemplate.setEnabled(0);
        draftTemplate.setPublishStatus("DRAFT");
        draftTemplate.setUpdatedAt(LocalDateTime.of(2026, 4, 3, 9, 30));

        when(categoryMapper.selectList(any())).thenReturn(List.of(category));
        when(templateMapper.selectList(any())).thenReturn(List.of(enabledTemplate, draftTemplate));
        when(processFormDesignService.listFormDesigns(null)).thenReturn(List.of());
        when(processExpenseDetailDesignService.listExpenseDetailDesigns()).thenReturn(List.of());

        ProcessCenterOverviewVO overview = service.getOverview();

        assertEquals(2, overview.getSummary().getTotalTemplates());
        assertEquals(1, overview.getSummary().getEnabledTemplates());
        assertEquals(1, overview.getSummary().getDraftTemplates());
        assertEquals("DRAFT", overview.getCategories().get(0).getTemplates().get(1).getStatus());
        assertEquals("\u8349\u7a3f", overview.getCategories().get(0).getTemplates().get(1).getStatusLabel());
    }

}
