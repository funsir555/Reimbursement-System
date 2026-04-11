package com.finex.auth.service.impl.financearchive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSummaryVO;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceVendorQueryDomainSupportTest {

    @Mock
    private FinanceVendorMapper financeVendorMapper;

    @Mock
    private UserMapper userMapper;

    private FinanceVendorQueryDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new FinanceVendorQueryDomainSupport(financeVendorMapper, userMapper, new ObjectMapper());
    }

    @Test
    void listVendorsReturnsCompanyScopedResults() {
        FinanceVendor vendor = new FinanceVendor();
        vendor.setCVenCode("VEN001");
        vendor.setCVenName("Shanghai Vendor");
        vendor.setCompanyId("COMPANY_A");

        when(financeVendorMapper.selectList(any())).thenReturn(List.of(vendor));

        List<FinanceVendorSummaryVO> result = support.listVendors("COMPANY_A", "Shanghai", false);

        assertEquals(1, result.size());
        assertEquals("COMPANY_A", result.get(0).getCompanyId());
        assertEquals("Shanghai Vendor", result.get(0).getCVenName());
    }

    @Test
    void getVendorDetailRejectsCrossCompanyAccess() {
        FinanceVendor vendor = new FinanceVendor();
        vendor.setCVenCode("VEN001");
        vendor.setCompanyId("COMPANY_B");

        when(financeVendorMapper.selectById("VEN001")).thenReturn(vendor);

        assertThrows(SecurityException.class, () -> support.getVendorDetail("COMPANY_A", "VEN001"));
    }

    @Test
    void getVendorDetailMapsActiveFlag() {
        FinanceVendor vendor = new FinanceVendor();
        vendor.setCVenCode("VEN001");
        vendor.setCVenName("Shanghai Vendor");
        vendor.setCompanyId("COMPANY_A");

        when(financeVendorMapper.selectById("VEN001")).thenReturn(vendor);

        FinanceVendorDetailVO result = support.getVendorDetail("COMPANY_A", "VEN001");

        assertEquals("COMPANY_A", result.getCompanyId());
        assertEquals(Boolean.TRUE, result.getActive());
    }
}