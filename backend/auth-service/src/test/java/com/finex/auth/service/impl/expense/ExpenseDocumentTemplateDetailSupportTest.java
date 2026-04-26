package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ProcessCustomArchiveDetailVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.impl.ExpenseDetailSystemFieldSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentTemplateDetailSupportTest {

    @Mock private AbstractExpenseDocumentSupport support;
    @Mock private ExpenseDetailSystemFieldSupport expenseDetailSystemFieldSupport;
    @Mock private UserMapper userMapper;
    @Mock private SystemCompanyMapper systemCompanyMapper;
    @Mock private SystemDepartmentMapper systemDepartmentMapper;

    @Test
    void getTemplateDetailBuildsTemplateRuntimePayload() {
        ExpenseDocumentTemplateDetailSupport detailSupport = newSupport();
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateCode("TPL-1");
        template.setTemplateName("Travel");
        template.setTemplateType("report");
        template.setTemplateTypeLabel("????");
        template.setCategoryCode("TRAVEL");
        template.setTemplateDescription("desc");
        template.setFormDesignCode("FORM-1");
        template.setApprovalFlow("FLOW-1");
        template.setFlowName("???");
        template.setExpenseDetailDesignCode("DETAIL-1");
        template.setExpenseDetailModeDefault("NORMAL");
        ProcessFormDesign formDesign = new ProcessFormDesign();
        formDesign.setFormName("??A");
        formDesign.setSchemaJson("{}");
        ProcessExpenseDetailDesign expenseDetailDesign = new ProcessExpenseDetailDesign();
        expenseDetailDesign.setDetailName("????A");
        expenseDetailDesign.setDetailType("ENTERPRISE");
        expenseDetailDesign.setSchemaJson("{}");
        User user = new User();
        user.setId(1L);
        user.setCompanyId("COMPANY_A");
        user.setDeptId(10L);
        SystemCompany company = new SystemCompany();
        company.setCompanyName("Company A");
        SystemDepartment department = new SystemDepartment();
        department.setDeptName("Finance");
        List<ProcessCustomArchiveDetailVO> archives = List.of(new ProcessCustomArchiveDetailVO());
        List<ProcessFormOptionVO> companyOptions = List.of(new ProcessFormOptionVO());
        List<ProcessFormOptionVO> departmentOptions = List.of(new ProcessFormOptionVO());
        List<ProcessFormOptionVO> userOptions = List.of(new ProcessFormOptionVO());
        List<ProcessFormOptionVO> expenseTypeOptions = List.of(new ProcessFormOptionVO());
        Map<String, Object> flowSnapshot = Map.of("nodes", List.of());
        Map<String, Object> schema = Map.of("blocks", List.of());

        when(support.requireTemplate("TPL-1")).thenReturn(template);
        when(support.resolveTemplateTypeLabel("report", "????")).thenReturn("????");
        when(support.resolveFlowSnapshotJson(template)).thenReturn("{}");
        when(support.readMap("{}")).thenReturn(flowSnapshot);
        when(support.loadFormDesign("FORM-1")).thenReturn(formDesign);
        when(support.readSchema("{}")).thenReturn(schema);
        when(support.loadSharedArchives(schema)).thenReturn(archives);
        when(support.loadExpenseDetailDesign("DETAIL-1")).thenReturn(expenseDetailDesign);
        when(support.resolveExpenseDetailTypeLabel("ENTERPRISE")).thenReturn("????");
        when(expenseDetailSystemFieldSupport.readSchema("{}", "ENTERPRISE")).thenReturn(schema);
        when(support.loadCompanyOptions()).thenReturn(companyOptions);
        when(support.loadDepartmentOptions()).thenReturn(departmentOptions);
        when(support.loadUserOptions(flowSnapshot)).thenReturn(userOptions);
        when(expenseDetailSystemFieldSupport.loadExpenseTypeOptions()).thenReturn(expenseTypeOptions);
        when(expenseDetailSystemFieldSupport.loadExpenseTypeInvoiceFreeModeMap()).thenReturn(Map.of("T1", "true"));
        when(userMapper.selectById(1L)).thenReturn(user);
        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(systemDepartmentMapper.selectById(10L)).thenReturn(department);
        when(support.trimToNull("COMPANY_A")).thenReturn("COMPANY_A");
        when(support.trimToNull("Company A")).thenReturn("Company A");

        ExpenseCreateTemplateDetailVO actual = detailSupport.getTemplateDetail(1L, "TPL-1");

        assertEquals("TPL-1", actual.getTemplateCode());
        assertEquals("Travel", actual.getTemplateName());
        assertEquals("????", actual.getTemplateTypeLabel());
        assertSame(flowSnapshot, actual.getFlowSnapshot());
        assertSame(schema, actual.getSchema());
        assertSame(archives, actual.getSharedArchives());
        assertEquals("????A", actual.getExpenseDetailDesignName());
        assertEquals("????", actual.getExpenseDetailTypeLabel());
        assertSame(companyOptions, actual.getCompanyOptions());
        assertSame(departmentOptions, actual.getDepartmentOptions());
        assertSame(userOptions, actual.getUserOptions());
        assertSame(expenseTypeOptions, actual.getExpenseTypeOptions());
        assertEquals("COMPANY_A", actual.getCurrentUserCompanyId());
        assertEquals("Company A", actual.getCurrentUserCompanyName());
        assertEquals("10", actual.getCurrentUserDeptId());
        assertEquals("Finance", actual.getCurrentUserDeptName());
    }

    private ExpenseDocumentTemplateDetailSupport newSupport() {
        return new ExpenseDocumentTemplateDetailSupport(
                support,
                expenseDetailSystemFieldSupport,
                userMapper,
                systemCompanyMapper,
                systemDepartmentMapper
        );
    }
}
