package com.finex.auth.service.impl.financearchive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceCustomerDetailVO;
import com.finex.auth.dto.FinanceCustomerSaveDTO;
import com.finex.auth.entity.FinanceCustomer;
import com.finex.auth.mapper.FinanceCustomerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceCustomerMutationDomainSupportTest {

    @Mock
    private FinanceCustomerMapper financeCustomerMapper;

    private FinanceCustomerMutationDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new FinanceCustomerMutationDomainSupport(financeCustomerMapper, new ObjectMapper());
    }

    @Test
    void createCustomerGeneratesCodeAndOverridesPayloadCompanyId() {
        FinanceCustomerSaveDTO dto = new FinanceCustomerSaveDTO();
        dto.setCCusName("????");
        dto.setCompanyId("COMPANY_B");

        FinanceCustomer persisted = new FinanceCustomer();
        persisted.setCCusCode("CUS202604110001");
        persisted.setCCusName("????");
        persisted.setCompanyId("COMPANY_A");

        when(financeCustomerMapper.selectCount(any())).thenReturn(0L);
        when(financeCustomerMapper.selectById("CUS202604110001")).thenReturn(null, persisted);
        when(financeCustomerMapper.insert(any(FinanceCustomer.class))).thenReturn(1);

        FinanceCustomerDetailVO result = support.createCustomer("COMPANY_A", dto, "tester");

        ArgumentCaptor<FinanceCustomer> captor = ArgumentCaptor.forClass(FinanceCustomer.class);
        verify(financeCustomerMapper).insert(captor.capture());
        FinanceCustomer inserted = captor.getValue();
        assertEquals("COMPANY_A", inserted.getCompanyId());
        assertEquals("CUS202604110001", inserted.getCCusCode());
        assertNotNull(inserted.getCreatedAt());
        assertNotNull(inserted.getUpdatedAt());
        assertEquals("COMPANY_A", result.getCompanyId());
    }

    @Test
    void createCustomerRejectsDuplicateExplicitCode() {
        FinanceCustomer existing = new FinanceCustomer();
        existing.setCCusCode("CUS001");

        FinanceCustomerSaveDTO dto = new FinanceCustomerSaveDTO();
        dto.setCCusCode("CUS001");
        dto.setCCusName("????");

        when(financeCustomerMapper.selectOne(any())).thenReturn(existing);

        assertThrows(IllegalStateException.class, () -> support.createCustomer("COMPANY_A", dto, "tester"));
    }

    @Test
    void updateCustomerKeepsCompanyIdStable() {
        FinanceCustomer existing = new FinanceCustomer();
        existing.setCCusCode("CUS001");
        existing.setCCusName("???");
        existing.setCompanyId("COMPANY_A");

        FinanceCustomer refreshed = new FinanceCustomer();
        refreshed.setCCusCode("CUS001");
        refreshed.setCCusName("???");
        refreshed.setCompanyId("COMPANY_A");

        FinanceCustomerSaveDTO dto = new FinanceCustomerSaveDTO();
        dto.setCCusCode("CUS001");
        dto.setCCusName("???");
        dto.setCompanyId("COMPANY_B");

        when(financeCustomerMapper.selectById("CUS001")).thenReturn(existing, refreshed);
        when(financeCustomerMapper.updateById(any(FinanceCustomer.class))).thenReturn(1);

        FinanceCustomerDetailVO result = support.updateCustomer("COMPANY_A", "CUS001", dto, "tester");

        ArgumentCaptor<FinanceCustomer> captor = ArgumentCaptor.forClass(FinanceCustomer.class);
        verify(financeCustomerMapper).updateById(captor.capture());
        assertEquals("COMPANY_A", captor.getValue().getCompanyId());
        assertEquals("COMPANY_A", result.getCompanyId());
    }

    @Test
    void disableCustomerWritesEndDateAndUpdatedAt() {
        FinanceCustomer existing = new FinanceCustomer();
        existing.setCCusCode("CUS001");
        existing.setCompanyId("COMPANY_A");

        when(financeCustomerMapper.selectById("CUS001")).thenReturn(existing);
        when(financeCustomerMapper.updateById(any(FinanceCustomer.class))).thenReturn(1);

        Boolean result = support.disableCustomer("COMPANY_A", "CUS001", "tester");

        ArgumentCaptor<FinanceCustomer> captor = ArgumentCaptor.forClass(FinanceCustomer.class);
        verify(financeCustomerMapper).updateById(captor.capture());
        assertNotNull(captor.getValue().getDEndDate());
        assertNotNull(captor.getValue().getUpdatedAt());
        assertEquals(Boolean.TRUE, result);
    }
}
