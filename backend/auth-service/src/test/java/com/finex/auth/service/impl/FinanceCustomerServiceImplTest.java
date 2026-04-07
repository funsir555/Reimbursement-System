package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceCustomerDetailVO;
import com.finex.auth.dto.FinanceCustomerSaveDTO;
import com.finex.auth.entity.FinanceCustomer;
import com.finex.auth.mapper.FinanceCustomerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceCustomerServiceImplTest {

    @Mock
    private FinanceCustomerMapper financeCustomerMapper;

    private FinanceCustomerServiceImpl financeCustomerService;

    @BeforeEach
    void setUp() {
        financeCustomerService = new FinanceCustomerServiceImpl(financeCustomerMapper, new ObjectMapper());
    }

    @Test
    void createCustomerUsesCurrentCompanyIdInsteadOfPayloadCompanyId() {
        FinanceCustomerSaveDTO dto = new FinanceCustomerSaveDTO();
        dto.setCCusCode("CUS202604050001");
        dto.setCCusName("广州客户");
        dto.setCompanyId("COMPANY_B");

        FinanceCustomer persisted = new FinanceCustomer();
        persisted.setCCusCode("CUS202604050001");
        persisted.setCCusName("广州客户");
        persisted.setCompanyId("COMPANY_A");

        when(financeCustomerMapper.selectById("CUS202604050001")).thenReturn(null, persisted);
        when(financeCustomerMapper.insert(any(FinanceCustomer.class))).thenReturn(1);

        FinanceCustomerDetailVO result = financeCustomerService.createCustomer("COMPANY_A", dto, "财务");

        assertEquals("COMPANY_A", result.getCompanyId());
        verify(financeCustomerMapper).insert(any(FinanceCustomer.class));
    }

    @Test
    void getCustomerDetailRejectsCrossCompanyAccess() {
        FinanceCustomer customer = new FinanceCustomer();
        customer.setCCusCode("CUS202604050001");
        customer.setCCusName("广州客户");
        customer.setCompanyId("COMPANY_B");

        when(financeCustomerMapper.selectById("CUS202604050001")).thenReturn(customer);

        assertThrows(SecurityException.class, () -> financeCustomerService.getCustomerDetail("COMPANY_A", "CUS202604050001"));
    }
}
