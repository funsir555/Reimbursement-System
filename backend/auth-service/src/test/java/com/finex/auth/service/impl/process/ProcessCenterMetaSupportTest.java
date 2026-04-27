package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finex.auth.dto.ProcessCenterOverviewVO;
import com.finex.auth.dto.ProcessExpenseDetailDesignSummaryVO;
import com.finex.auth.dto.ProcessFormDesignSummaryVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.dto.ProcessTemplateFormOptionsVO;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseType;
import com.finex.auth.entity.ProcessTemplateCategory;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessTemplateCategoryMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.service.ProcessExpenseDetailDesignService;
import com.finex.auth.service.ProcessFlowDesignService;
import com.finex.auth.service.ProcessFormDesignService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessCenterMetaSupportTest {

    @Mock private ProcessTemplateCategoryMapper categoryMapper;
    @Mock private ProcessDocumentTemplateMapper templateMapper;
    @Mock private ProcessCustomArchiveDesignMapper customArchiveDesignMapper;
    @Mock private ProcessExpenseTypeMapper processExpenseTypeMapper;
    @Mock private SystemDepartmentMapper systemDepartmentMapper;
    @Mock private ProcessFormDesignService processFormDesignService;
    @Mock private ProcessExpenseDetailDesignService processExpenseDetailDesignService;
    @Mock private ProcessFlowDesignService processFlowDesignService;

    private ProcessCenterMetaSupport support;

    @BeforeEach
    void setUp() {
        initTableInfo(ProcessTemplateCategory.class);
        initTableInfo(ProcessDocumentTemplate.class);
        initTableInfo(ProcessExpenseType.class);
        initTableInfo(SystemDepartment.class);
        initTableInfo(ProcessCustomArchiveDesign.class);
        support = new ProcessCenterMetaSupport(
                categoryMapper,
                templateMapper,
                customArchiveDesignMapper,
                processExpenseTypeMapper,
                systemDepartmentMapper,
                processFormDesignService,
                processExpenseDetailDesignService,
                processFlowDesignService
        );
    }

    private void initTableInfo(Class<?> entityClass) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entityClass);
    }

    @Test
    void getOverviewBuildsCategoryCardsWithBoundMetadata() {
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
        template.setHighlights("移动端提单|AI审核");
        template.setApprovalFlow("FLOW-001");
        template.setFlowName("差旅审批流程");
        template.setFormDesignCode("FD-001");
        template.setExpenseDetailDesignCode("EDD-001");
        template.setOwnerName("流程管理员");
        template.setEnabled(1);
        template.setPublishStatus("ENABLED");
        template.setUpdatedAt(LocalDateTime.of(2026, 4, 2, 9, 30));

        ProcessFormDesignSummaryVO formDesign = new ProcessFormDesignSummaryVO();
        formDesign.setFormCode("FD-001");
        formDesign.setFormName("差旅报销表单");

        ProcessExpenseDetailDesignSummaryVO expenseDetailDesign = new ProcessExpenseDetailDesignSummaryVO();
        expenseDetailDesign.setDetailCode("EDD-001");
        expenseDetailDesign.setDetailName("费用明细表单");

        when(categoryMapper.selectList(any())).thenReturn(List.of(category));
        when(templateMapper.selectList(any())).thenReturn(List.of(template));
        when(processFormDesignService.listFormDesigns(null)).thenReturn(List.of(formDesign));
        when(processExpenseDetailDesignService.listExpenseDetailDesigns()).thenReturn(List.of(expenseDetailDesign));

        ProcessCenterOverviewVO overview = support.getOverview();

        assertEquals(1, overview.getCategories().size());
        assertEquals("FLOW-001", overview.getCategories().get(0).getTemplates().get(0).getFlowCode());
        assertEquals("FD-001", overview.getCategories().get(0).getTemplates().get(0).getFormCode());
        assertEquals("差旅报销表单", overview.getCategories().get(0).getTemplates().get(0).getFormName());
        assertEquals("费用明细表单", overview.getCategories().get(0).getTemplates().get(0).getExpenseDetailDesignName());
        assertEquals(6, overview.getNavItems().size());
    }

    @Test
    void getTemplateTypesIncludesContractType() {
        assertTrue(
                support.getTemplateTypes().stream().anyMatch(item ->
                        "contract".equals(item.getCode())
                                && "合同单".equals(item.getName())
                                && "合同管理".equals(item.getSubtitle())
                )
        );
    }

    @Test
    void getFormOptionsIncludesPublishedFlowsAndArchiveOptions() {
        ProcessExpenseType expenseType = new ProcessExpenseType();
        expenseType.setId(1L);
        expenseType.setExpenseCode("TRAVEL");
        expenseType.setExpenseName("差旅费");
        expenseType.setStatus(1);

        SystemDepartment department = new SystemDepartment();
        department.setId(10L);
        department.setDeptName("财务部");
        department.setStatus(1);

        ProcessCustomArchiveDesign archive = new ProcessCustomArchiveDesign();
        archive.setArchiveCode("PROCESS_TAG_OPTIONS");
        archive.setArchiveName("标签档案");
        archive.setStatus(1);

        when(processFormDesignService.listFormDesignOptions("report"))
                .thenReturn(List.of(option("报销主表", "FD-001")));
        when(processExpenseDetailDesignService.listExpenseDetailDesigns())
                .thenReturn(List.of(detailOption("费用明细", "EDD-001")));
        when(processFlowDesignService.listPublishedFlowOptions())
                .thenReturn(List.of(option("差旅审批流程", "FLOW-001")));
        when(processExpenseTypeMapper.selectList(any())).thenReturn(List.of(expenseType));
        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of(department));
        when(customArchiveDesignMapper.selectList(any())).thenReturn(List.of(archive));

        ProcessTemplateFormOptionsVO options = support.getFormOptions("report");

        assertEquals("报销单", options.getTemplateTypeLabel());
        assertEquals("FLOW-001", options.getApprovalFlows().get(0).getValue());
        assertEquals("TRAVEL", options.getExpenseTypes().get(0).getExpenseCode());
        assertEquals("财务部", options.getDepartmentOptions().get(0).getLabel());
        assertEquals("PROCESS_TAG_OPTIONS", options.getTagOptions().get(0).getValue());
        assertEquals("PROCESS_TAG_OPTIONS", options.getInstallmentOptions().get(0).getValue());
    }

    private static ProcessFormOptionVO option(String label, String value) {
        ProcessFormOptionVO option = new ProcessFormOptionVO();
        option.setLabel(label);
        option.setValue(value);
        return option;
    }

    private static ProcessExpenseDetailDesignSummaryVO detailOption(String name, String code) {
        ProcessExpenseDetailDesignSummaryVO detail = new ProcessExpenseDetailDesignSummaryVO();
        detail.setDetailName(name);
        detail.setDetailCode(code);
        return detail;
    }
}
