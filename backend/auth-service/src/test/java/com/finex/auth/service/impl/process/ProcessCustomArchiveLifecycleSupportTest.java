package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessCustomArchiveDetailVO;
import com.finex.auth.dto.ProcessCustomArchiveItemDTO;
import com.finex.auth.dto.ProcessCustomArchiveMetaVO;
import com.finex.auth.dto.ProcessCustomArchiveResolveDTO;
import com.finex.auth.dto.ProcessCustomArchiveResolveResultVO;
import com.finex.auth.dto.ProcessFlowMetaVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.dto.ProcessCustomArchiveRuleDTO;
import com.finex.auth.dto.ProcessCustomArchiveSaveDTO;
import com.finex.auth.dto.ProcessCustomArchiveSummaryVO;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessCustomArchiveItem;
import com.finex.auth.entity.ProcessCustomArchiveRule;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessCustomArchiveLifecycleSupportTest {

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

    private ProcessCustomArchiveLifecycleSupport support;

    @BeforeEach
    void setUp() {
        initTableInfo(ProcessTemplateCategory.class);
        initTableInfo(ProcessDocumentTemplate.class);
        initTableInfo(ProcessTemplateScope.class);
        initTableInfo(ProcessCustomArchiveDesign.class);
        initTableInfo(ProcessCustomArchiveItem.class);
        initTableInfo(ProcessCustomArchiveRule.class);
        initTableInfo(ProcessExpenseType.class);
        initTableInfo(SystemDepartment.class);
        support = new ProcessCustomArchiveLifecycleSupport(
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
    void listCustomArchivesBuildsSummaryWithItemCounts() {
        ProcessCustomArchiveDesign archive = new ProcessCustomArchiveDesign();
        archive.setId(10L);
        archive.setArchiveCode("CA202604260001");
        archive.setArchiveName("标签档案");
        archive.setArchiveType("SELECT");
        archive.setArchiveDescription("标签来源");
        archive.setStatus(1);

        ProcessCustomArchiveItem first = new ProcessCustomArchiveItem();
        first.setArchiveId(10L);
        ProcessCustomArchiveItem second = new ProcessCustomArchiveItem();
        second.setArchiveId(10L);

        when(customArchiveDesignMapper.selectList(any())).thenReturn(List.of(archive));
        when(customArchiveItemMapper.selectList(any())).thenReturn(List.of(first, second));

        List<ProcessCustomArchiveSummaryVO> result = support.listCustomArchives();

        assertEquals(1, result.size());
        assertEquals("标签档案", result.get(0).getArchiveName());
        assertEquals("提供选择", result.get(0).getArchiveTypeLabel());
        assertEquals(2, result.get(0).getItemCount());
    }

    @Test
    void getCustomArchiveMetaIncludesOperatorsAndDepartments() {
        ProcessFlowMetaVO flowMeta = new ProcessFlowMetaVO();
        flowMeta.setDepartmentOptions(List.of(option("财务部", "20")));
        when(processFlowDesignService.getFlowMeta()).thenReturn(flowMeta);

        ProcessCustomArchiveMetaVO meta = support.getCustomArchiveMeta();

        assertEquals("SELECT", meta.getArchiveTypeOptions().get(0).getValue());
        assertEquals("EQ", meta.getOperatorOptions().get(0).getKey());
        assertEquals("submitterDeptId", meta.getRuleFields().get(0).getKey());
        assertEquals("财务部", meta.getDepartmentOptions().get(0).getLabel());
    }

    @Test
    void resolveCustomArchiveFiltersAutoRuleItemsByContext() {
        ProcessCustomArchiveDesign archive = new ProcessCustomArchiveDesign();
        archive.setId(11L);
        archive.setArchiveCode("PROCESS_TAG_OPTIONS");
        archive.setArchiveType("AUTO_RULE");

        ProcessCustomArchiveItem item = new ProcessCustomArchiveItem();
        item.setId(101L);
        item.setArchiveId(11L);
        item.setItemCode("ITEM-001");
        item.setItemName("高额报销");
        item.setPriority(1);
        item.setStatus(1);

        ProcessCustomArchiveRule rule = new ProcessCustomArchiveRule();
        rule.setArchiveItemId(101L);
        rule.setGroupNo(1);
        rule.setFieldKey("amount");
        rule.setOperator("GE");
        rule.setCompareValue("1000");

        ProcessCustomArchiveResolveDTO dto = new ProcessCustomArchiveResolveDTO();
        dto.setArchiveCode("PROCESS_TAG_OPTIONS");
        dto.setContext(Map.of("amount", 1200));

        when(customArchiveDesignMapper.selectOne(any())).thenReturn(archive);
        when(customArchiveItemMapper.selectList(any())).thenReturn(List.of(item));
        when(customArchiveRuleMapper.selectList(any())).thenReturn(List.of(rule));

        ProcessCustomArchiveResolveResultVO result = support.resolveCustomArchive(dto);

        assertEquals("AUTO_RULE", result.getArchiveType());
        assertEquals(1, result.getItems().size());
        assertEquals("ITEM-001", result.getItems().get(0).getItemCode());
    }

    @Test
    void createCustomArchivePersistsItemsAndRules() {
        ProcessCustomArchiveItemDTO item = new ProcessCustomArchiveItemDTO();
        item.setItemName("高额报销");
        item.setPriority(1);
        item.setStatus(1);

        ProcessCustomArchiveRuleDTO rule = new ProcessCustomArchiveRuleDTO();
        rule.setGroupNo(1);
        rule.setFieldKey("amount");
        rule.setOperator("GE");
        rule.setCompareValue(1000);
        item.setRules(List.of(rule));

        ProcessCustomArchiveSaveDTO dto = new ProcessCustomArchiveSaveDTO();
        dto.setArchiveName("标签档案");
        dto.setArchiveType("AUTO_RULE");
        dto.setArchiveDescription("标签来源");
        dto.setStatus(1);
        dto.setItems(List.of(item));

        ProcessCustomArchiveDesign created = new ProcessCustomArchiveDesign();
        created.setId(12L);
        created.setArchiveCode("CA202604260001");
        created.setArchiveName("标签档案");
        created.setArchiveType("AUTO_RULE");
        created.setArchiveDescription("标签来源");
        created.setStatus(1);

        doAnswer(invocation -> {
            ProcessCustomArchiveDesign inserted = invocation.getArgument(0);
            inserted.setId(12L);
            return 1;
        }).when(customArchiveDesignMapper).insert(any(ProcessCustomArchiveDesign.class));
        doAnswer(invocation -> {
            ProcessCustomArchiveItem inserted = invocation.getArgument(0);
            inserted.setId(101L);
            return 1;
        }).when(customArchiveItemMapper).insert(any(ProcessCustomArchiveItem.class));
        when(customArchiveDesignMapper.selectCount(any())).thenReturn(0L);
        when(customArchiveItemMapper.selectCount(any())).thenReturn(0L);
        when(customArchiveDesignMapper.selectById(12L)).thenReturn(created);
        when(customArchiveItemMapper.selectList(any())).thenReturn(List.of(itemEntity(101L, 12L, "CI202604260001", "高额报销")));
        when(customArchiveRuleMapper.selectList(any())).thenReturn(List.of(ruleEntity(101L, 1, "amount", "GE", "1000")));

        ProcessCustomArchiveDetailVO result = support.createCustomArchive(dto);

        assertEquals("标签档案", result.getArchiveName());
        assertEquals(1, result.getItems().size());
        assertEquals("高额报销", result.getItems().get(0).getItemName());
        verify(customArchiveRuleMapper).insert(any(ProcessCustomArchiveRule.class));
    }

    @Test
    void deleteCustomArchiveRejectsWhenResultItemsAlreadyReferenced() {
        ProcessCustomArchiveDesign archive = new ProcessCustomArchiveDesign();
        archive.setId(13L);
        archive.setArchiveCode("PROCESS_TAG_OPTIONS");

        ProcessCustomArchiveItem item = new ProcessCustomArchiveItem();
        item.setArchiveId(13L);
        item.setItemCode("ITEM-001");

        when(customArchiveDesignMapper.selectById(13L)).thenReturn(archive);
        when(customArchiveItemMapper.selectList(any())).thenReturn(List.of(item));
        when(scopeMapper.selectCount(any())).thenReturn(1L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> support.deleteCustomArchive(13L));

        assertEquals("当前档案结果项已被模板引用，不能删除档案", ex.getMessage());
        verify(customArchiveDesignMapper, never()).deleteById(anyLong());
    }

    private static ProcessCustomArchiveItem itemEntity(Long id, Long archiveId, String itemCode, String itemName) {
        ProcessCustomArchiveItem item = new ProcessCustomArchiveItem();
        item.setId(id);
        item.setArchiveId(archiveId);
        item.setItemCode(itemCode);
        item.setItemName(itemName);
        item.setPriority(1);
        item.setStatus(1);
        return item;
    }

    private static ProcessCustomArchiveRule ruleEntity(Long archiveItemId, Integer groupNo, String fieldKey, String operator, String compareValue) {
        ProcessCustomArchiveRule rule = new ProcessCustomArchiveRule();
        rule.setArchiveItemId(archiveItemId);
        rule.setGroupNo(groupNo);
        rule.setFieldKey(fieldKey);
        rule.setOperator(operator);
        rule.setCompareValue(compareValue);
        return rule;
    }

    private static ProcessFormOptionVO option(String label, String value) {
        ProcessFormOptionVO option = new ProcessFormOptionVO();
        option.setLabel(label);
        option.setValue(value);
        return option;
    }

    private void initTableInfo(Class<?> entityClass) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entityClass);
    }
}
