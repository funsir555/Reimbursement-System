package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.SystemCompanyMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentCompanyResolverTest {
    @Mock private SystemCompanyMapper companyMapper;

    @Test
    void paymentCompanyWinsEvenForApplicationAndDifferentSubmitterCompany() {
        when(companyMapper.selectById("PAYER")).thenReturn(active("PAYER"));
        assertEquals("PAYER", resolver().resolve(schema("pay"), Map.of("pay", "PAYER"), submitter("HOME")));
    }

    @Test
    void applicationWithoutPaymentCompanyUsesSubmitterCompany() {
        when(companyMapper.selectById("HOME")).thenReturn(active("HOME"));
        assertEquals("HOME", resolver().resolve(Map.of("blocks", List.of()), Map.of(), submitter("HOME")));
    }

    @Test
    void paymentCompanyLookupObjectUsesItsIdInsteadOfDisplayLabel() {
        when(companyMapper.selectById("PAYER")).thenReturn(active("PAYER"));
        assertEquals("PAYER", resolver().resolve(schema("pay"),
                Map.of("pay", Map.of("value", "PAYER", "label", "显示名称")), submitter("HOME")));
    }

    @Test
    void invalidOrAmbiguousPaymentCompanyNeverFallsBackToSubmitter() {
        assertThrows(IllegalArgumentException.class,
                () -> resolver().resolve(schema("pay"), Map.of("pay", "UNKNOWN"), submitter("HOME")));
        assertThrows(IllegalArgumentException.class,
                () -> resolver().resolve(schema("pay"), Map.of("pay", Map.of("label", "某公司")), submitter("HOME")));
        assertThrows(IllegalArgumentException.class,
                () -> resolver().resolve(schema("pay"), Map.of("pay", List.of("A", "B")), submitter("HOME")));
        assertThrows(IllegalArgumentException.class,
                () -> resolver().resolve(Map.of("blocks", List.of(block("first"), block("second"))),
                        Map.of("first", "A", "second", "B"), submitter("HOME")));
    }

    private ExpenseDocumentCompanyResolver resolver() {
        return new ExpenseDocumentCompanyResolver(companyMapper);
    }

    private Map<String, Object> schema(String key) {
        return Map.of("blocks", List.of(block(key)));
    }

    private Map<String, Object> block(String key) {
        return Map.of("kind", "BUSINESS_COMPONENT", "fieldKey", key,
                "props", Map.of("componentCode", "payment-company"));
    }

    private User submitter(String companyId) {
        User user = new User();
        user.setCompanyId(companyId);
        return user;
    }

    private SystemCompany active(String companyId) {
        SystemCompany company = new SystemCompany();
        company.setCompanyId(companyId);
        company.setStatus(1);
        return company;
    }
}
