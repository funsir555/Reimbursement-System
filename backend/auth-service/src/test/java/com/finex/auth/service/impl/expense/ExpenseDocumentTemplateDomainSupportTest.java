package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentTemplateDomainSupportTest {

    @Mock
    private AbstractExpenseDocumentSupport support;
    @Mock
    private ExpenseDocumentReadSupport readSupport;

    @Test
    void listAvailableTemplatesDelegatesToSharedSupport() {
        ExpenseDocumentTemplateDomainSupport domainSupport = new ExpenseDocumentTemplateDomainSupport(support, readSupport);
        List<ExpenseCreateTemplateSummaryVO> expected = List.of(new ExpenseCreateTemplateSummaryVO());
        when(support.listAvailableTemplates()).thenReturn(expected);

        List<ExpenseCreateTemplateSummaryVO> actual = domainSupport.listAvailableTemplates();

        assertSame(expected, actual);
        verify(support).listAvailableTemplates();
    }

    @Test
    void getDocumentEditContextBuildsFromTemplateAndReadSide() {
        ExpenseDocumentTemplateDomainSupport domainSupport = new ExpenseDocumentTemplateDomainSupport(support, readSupport);
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-1");
        instance.setTemplateCode("TPL-1");
        instance.setFormDataJson("{}");
        ExpenseCreateTemplateDetailVO templateDetail = new ExpenseCreateTemplateDetailVO();
        templateDetail.setTemplateCode("TPL-1");
        templateDetail.setTemplateName("Travel");
        templateDetail.setTemplateType("report");
        templateDetail.setCurrentUserCompanyId("COMPANY_A");
        templateDetail.setCurrentUserCompanyName("广州远智教育科技有限公司");
        ProcessDocumentExpenseDetail detail = new ProcessDocumentExpenseDetail();
        ExpenseDetailInstanceDTO runtimeDetail = new ExpenseDetailInstanceDTO();
        runtimeDetail.setDetailNo("D1");
        when(readSupport.requireDocument("DOC-1")).thenReturn(instance);
        when(support.getTemplateDetail(1L, "TPL-1")).thenReturn(templateDetail);
        when(readSupport.readFormData("{}")).thenReturn(Map.of("reason", "trip"));
        when(readSupport.loadExpenseDetails("DOC-1")).thenReturn(List.of(detail));
        when(readSupport.toRuntimeExpenseDetailDTO(detail)).thenReturn(runtimeDetail);

        ExpenseDocumentEditContextVO actual = domainSupport.getDocumentEditContext(1L, "DOC-1");

        assertEquals("RESUBMIT", actual.getEditMode());
        assertEquals("DOC-1", actual.getDocumentCode());
        assertEquals("TPL-1", actual.getTemplateCode());
        assertEquals("Travel", actual.getTemplateName());
        assertEquals("COMPANY_A", actual.getCurrentUserCompanyId());
        assertEquals("广州远智教育科技有限公司", actual.getCurrentUserCompanyName());
        assertEquals("trip", actual.getFormData().get("reason"));
        assertEquals(1, actual.getExpenseDetails().size());
        assertSame(runtimeDetail, actual.getExpenseDetails().get(0));
        verify(readSupport).requireSubmitter(instance, 1L);
    }
}
