package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessTemplateDetailVO;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessTemplateBindingSupportTest {

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

    private AbstractProcessTemplateSupport owner;
    private ProcessTemplateBindingSupport support;

    @BeforeEach
    void setUp() {
        initTableInfo(ProcessDocumentTemplate.class);
        initTableInfo(ProcessTemplateScope.class);
        initTableInfo(ProcessCustomArchiveDesign.class);
        initTableInfo(ProcessCustomArchiveItem.class);
        initTableInfo(ProcessExpenseType.class);
        initTableInfo(SystemDepartment.class);
        initTableInfo(ProcessTemplateCategory.class);

        owner = new ProcessTemplateDomainSupport(
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
        support = new ProcessTemplateBindingSupport(
                owner,
                scopeMapper,
                customArchiveDesignMapper,
                customArchiveItemMapper,
                processExpenseTypeMapper,
                systemDepartmentMapper,
                processFormDesignService,
                processExpenseDetailDesignService,
                processFlowDesignService
        );
    }

    @Test
    void buildTemplateDetailResolvesArchiveCodeFromLegacyItemScope() {
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setId(12L);
        template.setTemplateCode("FX202604270001");
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

        when(scopeMapper.selectList(any())).thenReturn(List.of(legacyTagScope));
        when(processExpenseDetailDesignService.resolveExpenseDetailType("EDD-001")).thenReturn("ENTERPRISE_TRANSACTION");
        when(customArchiveItemMapper.selectOne(any())).thenReturn(item);
        when(customArchiveDesignMapper.selectById(99L)).thenReturn(archive);

        ProcessTemplateDetailVO detail = support.buildTemplateDetail(template);

        assertEquals("PROCESS_TAG_OPTIONS", detail.getTagOption());
        assertEquals("ENTERPRISE_TRANSACTION", detail.getExpenseDetailType());
    }

    @Test
    void validateTemplateScopeRejectsExpenseDetailOnApplicationTemplate() {
        ProcessTemplateSaveDTO dto = new ProcessTemplateSaveDTO();
        dto.setTemplateType("application");
        dto.setTemplateName("业务申请");
        dto.setFormDesign("FD-001");
        dto.setApprovalFlow("FLOW-001");
        dto.setExpenseDetailDesign("EDD-001");

        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of());
        when(processExpenseTypeMapper.selectList(any())).thenReturn(List.of());
        when(processFormDesignService.resolveFormDesignCode("FD-001", "application")).thenReturn("FD-001");
        when(processFlowDesignService.publishedFlowLabelMap()).thenReturn(Map.of("FLOW-001", "申请流程"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> support.validateTemplateScope(dto));

        assertEquals("申请单和借款单不支持费用明细表单", ex.getMessage());
    }

    @Test
    void replaceTemplateScopesDeletesOldRowsAndPersistsAllBindings() {
        ProcessTemplateSaveDTO dto = new ProcessTemplateSaveDTO();
        dto.setScopeDeptIds(List.of("10"));
        dto.setScopeExpenseTypeCodes(List.of("TRAVEL"));
        dto.setAmountMin(new BigDecimal("100"));
        dto.setAmountMax(new BigDecimal("500"));
        dto.setTagOption("PROCESS_TAG_OPTIONS");
        dto.setInstallmentOption("PROCESS_INSTALLMENT_OPTIONS");

        support.replaceTemplateScopes(
                88L,
                dto,
                Map.of("10", "财务部"),
                Map.of("TRAVEL", "差旅费"),
                Map.of(
                        "PROCESS_TAG_OPTIONS", "标签档案",
                        "PROCESS_INSTALLMENT_OPTIONS", "分期档案"
                )
        );

        verify(scopeMapper).delete(any());
        verify(scopeMapper, times(6)).insert(any(ProcessTemplateScope.class));
    }

    private void initTableInfo(Class<?> entityClass) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entityClass);
    }
}
