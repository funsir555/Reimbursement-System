package com.finex.auth.service.impl.financearchive;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceCustomerDetailVO;
import com.finex.auth.dto.FinanceCustomerSummaryVO;
import com.finex.auth.entity.FinanceCustomer;
import com.finex.auth.mapper.FinanceCustomerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceCustomerQueryDomainSupportTest {

    @Mock
    private FinanceCustomerMapper financeCustomerMapper;

    private FinanceCustomerQueryDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new FinanceCustomerQueryDomainSupport(financeCustomerMapper, new ObjectMapper());
    }

    @Test
    void listCustomersBuildsQueryWithCompanyKeywordAndDisabledFilter() {
        FinanceCustomer customer = new FinanceCustomer();
        customer.setCCusCode("CUS001");
        customer.setCCusName("????");
        customer.setCompanyId("COMPANY_A");

        when(financeCustomerMapper.selectList(any())).thenReturn(List.of(customer));

        List<FinanceCustomerSummaryVO> result = support.listCustomers("COMPANY_A", "??", false);

        ArgumentCaptor<QueryWrapper<FinanceCustomer>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(financeCustomerMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("company_id"));
        assertTrue(sqlSegment.contains("cCusName") || sqlSegment.contains("cCusCode"));
        assertTrue(sqlSegment.contains("dEndDate"));
        assertEquals(1, result.size());
        assertTrue(result.get(0).getActive());
    }

    @Test
    void getCustomerDetailRejectsCrossCompanyAccess() {
        FinanceCustomer customer = new FinanceCustomer();
        customer.setCCusCode("CUS001");
        customer.setCCusName("????");
        customer.setCompanyId("COMPANY_B");

        when(financeCustomerMapper.selectById("CUS001")).thenReturn(customer);

        assertThrows(SecurityException.class, () -> support.getCustomerDetail("COMPANY_A", "CUS001"));
    }

    @Test
    void getCustomerDetailReturnsCompatibleReadModel() {
        FinanceCustomer customer = new FinanceCustomer();
        customer.setCCusCode("CUS001");
        customer.setCCusName("????");
        customer.setCompanyId("COMPANY_A");

        when(financeCustomerMapper.selectById("CUS001")).thenReturn(customer);

        FinanceCustomerDetailVO detail = support.getCustomerDetail("COMPANY_A", "CUS001");

        assertEquals("CUS001", detail.getCCusCode());
        assertEquals("COMPANY_A", detail.getCompanyId());
        assertFalse(Boolean.FALSE.equals(detail.getActive()));
    }
}
