package com.finex.auth.service.impl.financearchive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceVendorOptionDomainSupportTest {

    @Mock
    private FinanceVendorMapper financeVendorMapper;

    @Mock
    private UserMapper userMapper;

    private FinanceVendorOptionDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new FinanceVendorOptionDomainSupport(financeVendorMapper, userMapper, new ObjectMapper());
    }

    @Test
    void listActiveVendorOptionsUsesReceiptAccountNameAsCounterpartyLabel() {
        FinanceVendor current = new FinanceVendor();
        current.setCVenCode("VEN_A");
        current.setCVenName("Vendor A");
        current.setCVenAbbName("VA");
        current.setReceiptAccountName("Vendor A Receipt");
        current.setCompanyId("COMPANY_A");

        FinanceVendor other = new FinanceVendor();
        other.setCVenCode("VEN_B");
        other.setCVenName("Vendor B");
        other.setCompanyId("COMPANY_B");

        when(financeVendorMapper.selectList(any())).thenReturn(List.of(current, other));

        List<ExpenseCreateVendorOptionVO> result = support.listActiveVendorOptions("COMPANY_A", null, false);

        assertEquals(1, result.size());
        assertEquals("VEN_A", result.get(0).getValue());
        assertEquals("Vendor A Receipt", result.get(0).getLabel());
        assertEquals("VEN_A / Vendor A / VA", result.get(0).getSecondaryLabel());
    }
}
