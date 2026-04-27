package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessExpenseTypeDetailVO;
import com.finex.auth.dto.ProcessExpenseTypeMetaVO;
import com.finex.auth.dto.ProcessExpenseTypeSaveDTO;
import com.finex.auth.dto.ProcessExpenseTypeTreeVO;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessCustomArchiveItem;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseType;
import com.finex.auth.entity.ProcessTemplateCategory;
import com.finex.auth.entity.ProcessTemplateScope;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessExpenseTypeLifecycleSupportTest {

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

    private ProcessExpenseTypeLifecycleSupport support;

    @BeforeEach
    void setUp() {
        initTableInfo(ProcessTemplateCategory.class);
        initTableInfo(ProcessDocumentTemplate.class);
        initTableInfo(ProcessTemplateScope.class);
        initTableInfo(ProcessCustomArchiveDesign.class);
        initTableInfo(ProcessCustomArchiveItem.class);
        initTableInfo(ProcessExpenseType.class);
        initTableInfo(SystemDepartment.class);
        initTableInfo(User.class);
        support = new ProcessExpenseTypeLifecycleSupport(
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
    void listExpenseTypeTreeBuildsParentChildHierarchy() {
        ProcessExpenseType parent = new ProcessExpenseType();
        parent.setId(1L);
        parent.setExpenseCode("660201");
        parent.setExpenseName("差旅费");
        parent.setStatus(1);

        ProcessExpenseType child = new ProcessExpenseType();
        child.setId(2L);
        child.setParentId(1L);
        child.setExpenseCode("66020101");
        child.setExpenseName("机票");
        child.setStatus(1);

        when(processExpenseTypeMapper.selectList(any())).thenReturn(List.of(parent, child));

        List<ProcessExpenseTypeTreeVO> result = support.listExpenseTypeTree();

        assertEquals(1, result.size());
        assertEquals("660201", result.get(0).getExpenseCode());
        assertEquals(1, result.get(0).getChildren().size());
        assertEquals("66020101", result.get(0).getChildren().get(0).getExpenseCode());
    }

    @Test
    void getExpenseTypeMetaIncludesDepartmentAndUserOptions() {
        SystemDepartment department = new SystemDepartment();
        department.setId(10L);
        department.setDeptName("财务部");
        department.setStatus(1);

        User user = new User();
        user.setId(20L);
        user.setName("张三");
        user.setUsername("zhangsan");
        user.setStatus(1);

        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of(department));
        when(userMapper.selectList(any())).thenReturn(List.of(user));

        ProcessExpenseTypeMetaVO meta = support.getExpenseTypeMeta();

        assertEquals("财务部", meta.getDepartmentOptions().get(0).getLabel());
        assertEquals("张三 (zhangsan)", meta.getUserOptions().get(0).getLabel());
        assertEquals("FREE", meta.getInvoiceFreeOptions().get(0).getValue());
    }

    @Test
    void createExpenseTypePersistsParentAndReturnsDetail() {
        ProcessExpenseType parent = new ProcessExpenseType();
        parent.setId(1L);
        parent.setExpenseCode("660201");
        parent.setStatus(1);

        ProcessExpenseType created = new ProcessExpenseType();
        created.setId(2L);
        created.setParentId(1L);
        created.setExpenseCode("66020101");
        created.setExpenseName("机票");
        created.setExpenseDescription("国内机票");
        created.setCodeLevel(2);
        created.setCodePrefix("6602");
        created.setScopeDeptIds("[\"10\"]");
        created.setScopeUserIds("[\"20\"]");
        created.setInvoiceFreeMode("FREE");
        created.setTaxDeductionMode("DEFAULT");
        created.setTaxSeparationMode("SEPARATE");
        created.setStatus(1);

        ProcessExpenseTypeSaveDTO dto = new ProcessExpenseTypeSaveDTO();
        dto.setExpenseCode("66020101");
        dto.setExpenseName("机票");
        dto.setExpenseDescription("国内机票");
        dto.setScopeDeptIds(List.of("10"));
        dto.setScopeUserIds(List.of("20"));
        dto.setInvoiceFreeMode("FREE");
        dto.setTaxDeductionMode("DEFAULT");
        dto.setTaxSeparationMode("SEPARATE");
        dto.setStatus(1);

        SystemDepartment department = new SystemDepartment();
        department.setId(10L);
        department.setStatus(1);
        User user = new User();
        user.setId(20L);
        user.setStatus(1);

        when(processExpenseTypeMapper.selectOne(any())).thenReturn(null, parent, parent);
        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of(department));
        when(userMapper.selectList(any())).thenReturn(List.of(user));
        doAnswer(invocation -> {
            ProcessExpenseType inserted = invocation.getArgument(0);
            inserted.setId(2L);
            return 1;
        }).when(processExpenseTypeMapper).insert(any(ProcessExpenseType.class));
        when(processExpenseTypeMapper.selectById(2L)).thenReturn(created);

        ProcessExpenseTypeDetailVO result = support.createExpenseType(dto);

        assertEquals("66020101", result.getExpenseCode());
        assertEquals(2, result.getCodeLevel());
        assertEquals(List.of("10"), result.getScopeDeptIds());
    }

    @Test
    void deleteExpenseTypeRejectsWhenReferencedByTemplate() {
        ProcessExpenseType expenseType = new ProcessExpenseType();
        expenseType.setId(3L);
        expenseType.setExpenseCode("660201");

        when(processExpenseTypeMapper.selectById(3L)).thenReturn(expenseType);
        when(processExpenseTypeMapper.selectCount(any())).thenReturn(0L);
        when(scopeMapper.selectCount(any())).thenReturn(1L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> support.deleteExpenseType(3L));

        assertEquals("当前费用类型已被模板引用，不能删除", ex.getMessage());
        verify(processExpenseTypeMapper, never()).deleteById(anyLong());
    }

    private void initTableInfo(Class<?> entityClass) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entityClass);
    }
}
