package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSaveDTO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.User;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceVendorServiceImplTest {

    @Mock
    private FinanceVendorMapper financeVendorMapper;

    @Mock
    private UserMapper userMapper;

    private FinanceVendorServiceImpl financeVendorService;

    @BeforeEach
    void setUp() {
        financeVendorService = new FinanceVendorServiceImpl(financeVendorMapper, userMapper, new ObjectMapper());
    }

    @Test
    void createVendorUsesExplicitCompanyIdInsteadOfPayloadCompanyId() {
        FinanceVendorSaveDTO dto = new FinanceVendorSaveDTO();
        dto.setCVenCode("VEN202604050001");
        dto.setCVenName("Guangzhou Vendor");
        dto.setCompanyId("COMPANY_B");

        FinanceVendor persisted = new FinanceVendor();
        persisted.setCVenCode("VEN202604050001");
        persisted.setCVenName("Guangzhou Vendor");
        persisted.setCompanyId("COMPANY_A");

        when(financeVendorMapper.selectById("VEN202604050001")).thenReturn(null, persisted);
        when(financeVendorMapper.insert(any(FinanceVendor.class))).thenReturn(1);

        FinanceVendorDetailVO result = financeVendorService.createVendor("COMPANY_A", dto, "finance");

        assertEquals("COMPANY_A", result.getCompanyId());
        verify(financeVendorMapper).insert(any(FinanceVendor.class));
    }

    @Test
    void createVendorUsesCurrentUserCompanyIdInsteadOfPayloadCompanyId() {
        FinanceVendorSaveDTO dto = new FinanceVendorSaveDTO();
        dto.setCVenCode("VEN202604050002");
        dto.setCVenName("Quick Vendor");
        dto.setCompanyId("COMPANY_B");

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setCompanyId("COMPANY_A");

        FinanceVendor persisted = new FinanceVendor();
        persisted.setCVenCode("VEN202604050002");
        persisted.setCVenName("Quick Vendor");
        persisted.setCompanyId("COMPANY_A");

        when(userMapper.selectById(1L)).thenReturn(currentUser);
        when(financeVendorMapper.selectById("VEN202604050002")).thenReturn(null, persisted);
        when(financeVendorMapper.insert(any(FinanceVendor.class))).thenReturn(1);

        FinanceVendorDetailVO result = financeVendorService.createVendor(1L, dto, "tester");

        assertEquals("COMPANY_A", result.getCompanyId());
        verify(userMapper).selectById(1L);
    }

    @Test
    void listActiveVendorOptionsFiltersOutOtherCompanies() {
        FinanceVendor companyVendor = new FinanceVendor();
        companyVendor.setCVenCode("VEN_A");
        companyVendor.setCVenName("Company Vendor");
        companyVendor.setCompanyId("COMPANY_A");

        FinanceVendor otherVendor = new FinanceVendor();
        otherVendor.setCVenCode("VEN_B");
        otherVendor.setCVenName("Other Vendor");
        otherVendor.setCompanyId("COMPANY_B");

        when(financeVendorMapper.selectList(any())).thenReturn(List.of(companyVendor, otherVendor));

        List<ExpenseCreateVendorOptionVO> result = financeVendorService.listActiveVendorOptions("COMPANY_A", null);

        assertEquals(1, result.size());
        assertEquals("VEN_A", result.get(0).getCVenCode());
    }

    @Test
    void getVendorDetailRejectsCrossCompanyAccess() {
        FinanceVendor vendor = new FinanceVendor();
        vendor.setCVenCode("VEN202604050001");
        vendor.setCVenName("Guangzhou Vendor");
        vendor.setCompanyId("COMPANY_B");

        when(financeVendorMapper.selectById("VEN202604050001")).thenReturn(vendor);

        assertThrows(SecurityException.class, () -> financeVendorService.getVendorDetail("COMPANY_A", "VEN202604050001"));
    }
}
