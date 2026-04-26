package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.entity.ProcessDocumentInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentTemplateDomainSupportTest {

    @Mock private ExpenseDocumentTemplateListSupport templateListSupport;
    @Mock private ExpenseDocumentTemplateDetailSupport templateDetailSupport;
    @Mock private ExpenseDocumentCounterpartyOptionSupport counterpartyOptionSupport;
    @Mock private ExpenseDocumentEditContextSupport editContextSupport;

    @Test
    void listAvailableTemplatesDelegatesToDetailSupport() {
        ExpenseDocumentTemplateDomainSupport domainSupport = newSupport();
        List<ExpenseCreateTemplateSummaryVO> expected = List.of(new ExpenseCreateTemplateSummaryVO());
        when(templateListSupport.listAvailableTemplates()).thenReturn(expected);

        List<ExpenseCreateTemplateSummaryVO> actual = domainSupport.listAvailableTemplates();

        assertSame(expected, actual);
        verify(templateListSupport).listAvailableTemplates();
    }

    @Test
    void getTemplateDetailDelegatesToDetailSupport() {
        ExpenseDocumentTemplateDomainSupport domainSupport = newSupport();
        ExpenseCreateTemplateDetailVO expected = new ExpenseCreateTemplateDetailVO();
        when(templateDetailSupport.getTemplateDetail(1L, "TPL-1")).thenReturn(expected);

        ExpenseCreateTemplateDetailVO actual = domainSupport.getTemplateDetail(1L, "TPL-1");

        assertSame(expected, actual);
        verify(templateDetailSupport).getTemplateDetail(1L, "TPL-1");
    }

    @Test
    void listVendorOptionsDelegatesToCounterpartyOwner() {
        ExpenseDocumentTemplateDomainSupport domainSupport = newSupport();
        List<ExpenseCreateVendorOptionVO> expected = List.of(new ExpenseCreateVendorOptionVO());
        when(counterpartyOptionSupport.listVendorOptions(1L, "abc", false, "C1")).thenReturn(expected);

        List<ExpenseCreateVendorOptionVO> actual = domainSupport.listVendorOptions(1L, "abc", false, "C1");

        assertSame(expected, actual);
        verify(counterpartyOptionSupport).listVendorOptions(1L, "abc", false, "C1");
    }

    @Test
    void listPayeeOptionsDelegatesToCounterpartyOwner() {
        ExpenseDocumentTemplateDomainSupport domainSupport = newSupport();
        List<ExpenseCreatePayeeOptionVO> expected = List.of(new ExpenseCreatePayeeOptionVO());
        when(counterpartyOptionSupport.listPayeeOptions(1L, "abc", true)).thenReturn(expected);

        List<ExpenseCreatePayeeOptionVO> actual = domainSupport.listPayeeOptions(1L, "abc", true);

        assertSame(expected, actual);
        verify(counterpartyOptionSupport).listPayeeOptions(1L, "abc", true);
    }

    @Test
    void listPayeeAccountOptionsDelegatesToCounterpartyOwner() {
        ExpenseDocumentTemplateDomainSupport domainSupport = newSupport();
        List<ExpenseCreatePayeeAccountOptionVO> expected = List.of(new ExpenseCreatePayeeAccountOptionVO());
        when(counterpartyOptionSupport.listPayeeAccountOptions(1L, "abc", "EMPLOYEE", "??", "VEN-1", "C1")).thenReturn(expected);

        List<ExpenseCreatePayeeAccountOptionVO> actual = domainSupport.listPayeeAccountOptions(1L, "abc", "EMPLOYEE", "??", "VEN-1", "C1");

        assertSame(expected, actual);
        verify(counterpartyOptionSupport).listPayeeAccountOptions(1L, "abc", "EMPLOYEE", "??", "VEN-1", "C1");
    }

    @Test
    void editContextCallsDelegateOwners() {
        ExpenseDocumentTemplateDomainSupport domainSupport = newSupport();
        ExpenseDocumentEditContextVO expected = new ExpenseDocumentEditContextVO();
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        when(editContextSupport.getDocumentEditContext(1L, "DOC-1")).thenReturn(expected);
        when(editContextSupport.buildEditContext(1L, instance, 10L, "MODIFY")).thenReturn(expected);

        assertSame(expected, domainSupport.getDocumentEditContext(1L, "DOC-1"));
        assertSame(expected, domainSupport.buildEditContext(1L, instance, 10L, "MODIFY"));
        verify(editContextSupport).getDocumentEditContext(1L, "DOC-1");
        verify(editContextSupport).buildEditContext(1L, instance, 10L, "MODIFY");
    }

    private ExpenseDocumentTemplateDomainSupport newSupport() {
        return new ExpenseDocumentTemplateDomainSupport(
                templateListSupport,
                templateDetailSupport,
                counterpartyOptionSupport,
                editContextSupport
        );
    }
}
