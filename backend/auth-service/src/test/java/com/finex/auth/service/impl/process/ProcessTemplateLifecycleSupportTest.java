package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessTemplateDetailVO;
import com.finex.auth.dto.ProcessTemplateSaveDTO;
import com.finex.auth.dto.ProcessTemplateSaveResultVO;
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
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessTemplateLifecycleSupportTest {

    @Mock private ProcessTemplateCategoryMapper categoryMapper;
    @Mock private ProcessDocumentTemplateMapper templateMapper;
    @Mock private CodeSequenceMapper codeSequenceMapper;
    @Mock private ProcessTemplateScopeMapper scopeMapper;
    @Mock private ProcessCustomArchiveDesignMapper customArchiveDesignMapper;
    @Mock private ProcessCustomArchiveItemMapper customArchiveItemMapper;
    @Mock private ProcessCustomArchiveRuleMapper customArchiveRuleMapper;
    @Mock private ProcessExpenseTypeMapper processExpenseTypeMapper;
    @Mock private SystemDepartmentMapper systemDepartmentMapper;
    @Mock private UserMapper userMapper;
    @Mock private ProcessFormDesignService processFormDesignService;
    @Mock private ProcessExpenseDetailDesignService processExpenseDetailDesignService;
    @Mock private ProcessFlowDesignService processFlowDesignService;

    private ProcessTemplateLifecycleSupport support;

    @BeforeEach
    void setUp() {
        initTableInfo(ProcessDocumentTemplate.class);
        initTableInfo(ProcessTemplateScope.class);
        initTableInfo(ProcessCustomArchiveDesign.class);
        initTableInfo(ProcessCustomArchiveItem.class);
        initTableInfo(ProcessExpenseType.class);
        initTableInfo(SystemDepartment.class);
        initTableInfo(ProcessTemplateCategory.class);
        support = new ProcessTemplateLifecycleSupport(
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
    void getTemplateDetailResolvesArchiveCodeFromLegacyItemScope() {
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setId(12L);
        template.setTemplateCode("FX202604260001");
        template.setTemplateType("report");
        template.setTemplateTypeLabel("报销单");
        template.setTemplateName("差旅报销单");
        template.setCategoryCode("employee-expense");
        template.setExpenseDetailDesignCode("EDD-001");

        ProcessTemplateScope legacyTagScope = new ProcessTemplateScope();
        legacyTagScope.setTemplateId(12L);
        legacyTagScope.setOptionType("TAG_OPTION");
        legacyTagScope.setOptionCode("ITEM-001");
        legacyTagScope.setSortOrder(1);

        ProcessCustomArchiveItem item = new ProcessCustomArchiveItem();
        item.setArchiveId(99L);
        item.setItemCode("ITEM-001");

        ProcessCustomArchiveDesign archive = new ProcessCustomArchiveDesign();
        archive.setId(99L);
        archive.setArchiveCode("PROCESS_TAG_OPTIONS");

        when(templateMapper.selectById(12L)).thenReturn(template);
        when(scopeMapper.selectList(any())).thenReturn(List.of(legacyTagScope));
        when(processExpenseDetailDesignService.resolveExpenseDetailType("EDD-001")).thenReturn("ENTERPRISE_TRANSACTION");
        when(customArchiveItemMapper.selectOne(any())).thenReturn(item);
        when(customArchiveDesignMapper.selectById(99L)).thenReturn(archive);

        ProcessTemplateDetailVO detail = support.getTemplateDetail(12L);

        assertEquals("PROCESS_TAG_OPTIONS", detail.getTagOption());
        assertEquals("ENTERPRISE_TRANSACTION", detail.getExpenseDetailType());
    }

    @Test
    void saveTemplatePersistsTemplateAndAllScopes() {
        ProcessTemplateSaveDTO dto = new ProcessTemplateSaveDTO();
        dto.setTemplateType("report");
        dto.setTemplateName("差旅报销单");
        dto.setCategory("employee-expense");
        dto.setEnabled(Boolean.TRUE);
        dto.setFormDesign("FD-001");
        dto.setExpenseDetailDesign("EDD-001");
        dto.setApprovalFlow("FLOW-001");
        dto.setPaymentMode("public-payment");
        dto.setAllocationForm("allocation-default");
        dto.setAiAuditMode("standard");
        dto.setScopeDeptIds(List.of("10"));
        dto.setScopeExpenseTypeCodes(List.of("TRAVEL"));
        dto.setAmountMin(new BigDecimal("100"));
        dto.setAmountMax(new BigDecimal("500"));
        dto.setTagOption("PROCESS_TAG_OPTIONS");
        dto.setInstallmentOption("PROCESS_INSTALLMENT_OPTIONS");

        SystemDepartment department = new SystemDepartment();
        department.setId(10L);
        department.setDeptName("财务部");
        department.setStatus(1);

        ProcessExpenseType expenseType = new ProcessExpenseType();
        expenseType.setExpenseCode("TRAVEL");
        expenseType.setExpenseName("差旅费");
        expenseType.setStatus(1);

        ProcessCustomArchiveDesign tagArchive = new ProcessCustomArchiveDesign();
        tagArchive.setArchiveCode("PROCESS_TAG_OPTIONS");
        tagArchive.setArchiveName("标签档案");
        tagArchive.setStatus(1);

        ProcessCustomArchiveDesign installmentArchive = new ProcessCustomArchiveDesign();
        installmentArchive.setArchiveCode("PROCESS_INSTALLMENT_OPTIONS");
        installmentArchive.setArchiveName("分期档案");
        installmentArchive.setStatus(1);

        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of(department));
        when(processExpenseTypeMapper.selectList(any())).thenReturn(List.of(expenseType));
        when(processFormDesignService.resolveFormDesignCode("FD-001", "report")).thenReturn("FD-001");
        when(processExpenseDetailDesignService.resolveExpenseDetailDesignCode("EDD-001")).thenReturn("EDD-001");
        when(processExpenseDetailDesignService.resolveExpenseDetailType("EDD-001")).thenReturn("NORMAL");
        when(processFlowDesignService.publishedFlowLabelMap()).thenReturn(Map.of("FLOW-001", "差旅审批流程"));
        when(customArchiveDesignMapper.selectList(any())).thenReturn(List.of(tagArchive, installmentArchive));
        when(templateMapper.selectList(any())).thenReturn(List.of());
        when(codeSequenceMapper.allocateNextTemplateCodeValue(any(), any())).thenReturn(0, 1);
        when(templateMapper.selectMaxTemplateCodeValueByPrefix(any())).thenReturn(0L);
        when(codeSequenceMapper.currentAllocatedValue()).thenReturn(1L);
        when(templateMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            ProcessDocumentTemplate inserted = invocation.getArgument(0);
            inserted.setId(88L);
            return 1;
        }).when(templateMapper).insert(any(ProcessDocumentTemplate.class));

        ProcessTemplateSaveResultVO result = support.saveTemplate(dto, "Operator");

        assertNotNull(result);
        assertEquals(88L, result.getId());
        assertEquals("差旅报销单", result.getTemplateName());
        assertEquals("ENABLED", result.getStatus());
        verify(scopeMapper, times(6)).insert(any(ProcessTemplateScope.class));
    }

    @Test
    void copyTemplateCreatesDraftDuplicateWithCopiedBindings() {
        ProcessDocumentTemplate source = new ProcessDocumentTemplate();
        source.setId(12L);
        source.setTemplateName("差旅报销单");
        source.setTemplateType("report");
        source.setCategoryCode("employee-expense");
        source.setTemplateDescription("差旅费用报销");
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
        source.setOwnerName("流程管理员");

        when(templateMapper.selectById(12L)).thenReturn(source);
        when(scopeMapper.selectList(any())).thenReturn(List.of());
        when(processFormDesignService.resolveFormDesignCode("FD-001", "report")).thenReturn("FD-001");
        when(processExpenseDetailDesignService.resolveExpenseDetailDesignCode("EDD-001")).thenReturn("EDD-001");
        when(processExpenseDetailDesignService.resolveExpenseDetailType("EDD-001")).thenReturn("ENTERPRISE_TRANSACTION");
        when(processFlowDesignService.publishedFlowLabelMap()).thenReturn(Map.of("FLOW-001", "差旅审批流程"));
        when(customArchiveDesignMapper.selectList(any())).thenReturn(List.of());
        when(templateMapper.selectList(any())).thenReturn(List.of());
        when(codeSequenceMapper.allocateNextTemplateCodeValue(any(), any())).thenReturn(0, 1);
        when(templateMapper.selectMaxTemplateCodeValueByPrefix(any())).thenReturn(0L);
        when(codeSequenceMapper.currentAllocatedValue()).thenReturn(1L);
        when(templateMapper.selectCount(any())).thenReturn(0L, 0L);
        doAnswer(invocation -> {
            ProcessDocumentTemplate inserted = invocation.getArgument(0);
            inserted.setId(66L);
            return 1;
        }).when(templateMapper).insert(any(ProcessDocumentTemplate.class));

        ProcessTemplateSaveResultVO result = support.copyTemplate(12L, "Operator");

        assertEquals(66L, result.getId());
        assertEquals("DRAFT", result.getStatus());
        assertEquals("差旅报销单 - 副本", result.getTemplateName());
    }

    private void initTableInfo(Class<?> entityClass) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entityClass);
    }
}
