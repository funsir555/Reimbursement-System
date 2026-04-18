package com.finex.auth.service.impl.financearchive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSaveDTO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceVendorMutationDomainSupportTest {

    @Mock
    private FinanceVendorMapper financeVendorMapper;

    @Mock
    private UserMapper userMapper;

    private FinanceVendorMutationDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new FinanceVendorMutationDomainSupport(financeVendorMapper, userMapper, new ObjectMapper());
    }

    @Test
    void createVendorOverridesPayloadCompanyId() {
        FinanceVendorSaveDTO dto = new FinanceVendorSaveDTO();
        dto.setCVenCode("VEN202604110001");
        dto.setCVenName("Vendor A");
        dto.setCompanyId("COMPANY_B");

        FinanceVendor persisted = new FinanceVendor();
        persisted.setCVenCode("VEN202604110001");
        persisted.setCVenName("Vendor A");
        persisted.setCompanyId("COMPANY_A");

        when(financeVendorMapper.selectById("VEN202604110001")).thenReturn(null, persisted);
        when(financeVendorMapper.insert(any(FinanceVendor.class))).thenReturn(1);

        FinanceVendorDetailVO result = support.createVendor("COMPANY_A", dto, "tester", false);

        ArgumentCaptor<FinanceVendor> captor = ArgumentCaptor.forClass(FinanceVendor.class);
        verify(financeVendorMapper).insert(captor.capture());
        assertEquals("COMPANY_A", captor.getValue().getCompanyId());
        assertEquals("COMPANY_A", result.getCompanyId());
    }

    @Test
    void createVendorFromCurrentUserUsesResolvedCompanyId() {
        FinanceVendorSaveDTO dto = new FinanceVendorSaveDTO();
        dto.setCVenCode("VEN202604110002");
        dto.setCVenName("Vendor B");
        dto.setCompanyId("COMPANY_B");

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setCompanyId("COMPANY_A");

        FinanceVendor persisted = new FinanceVendor();
        persisted.setCVenCode("VEN202604110002");
        persisted.setCVenName("Vendor B");
        persisted.setCompanyId("COMPANY_A");

        when(userMapper.selectById(1L)).thenReturn(currentUser);
        when(financeVendorMapper.selectById("VEN202604110002")).thenReturn(null, persisted);
        when(financeVendorMapper.insert(any(FinanceVendor.class))).thenReturn(1);

        FinanceVendorDetailVO result = support.createVendor(1L, dto, "tester", false);

        assertEquals("COMPANY_A", result.getCompanyId());
    }

    @Test
    void updateVendorKeepsIdentityFieldsStable() {
        FinanceVendor existing = new FinanceVendor();
        existing.setCVenCode("VEN001");
        existing.setCVenName("Old Vendor");
        existing.setCompanyId("COMPANY_A");
        existing.setCCreatePerson("creator");

        FinanceVendor refreshed = new FinanceVendor();
        refreshed.setCVenCode("VEN001");
        refreshed.setCVenName("New Vendor");
        refreshed.setCompanyId("COMPANY_A");

        FinanceVendorSaveDTO dto = new FinanceVendorSaveDTO();
        dto.setCVenCode("VEN001");
        dto.setCVenName("New Vendor");
        dto.setCompanyId("COMPANY_B");

        when(financeVendorMapper.selectById("VEN001")).thenReturn(existing, refreshed);
        when(financeVendorMapper.updateById(any(FinanceVendor.class))).thenReturn(1);

        FinanceVendorDetailVO result = support.updateVendor("COMPANY_A", "VEN001", dto, "tester", false);

        ArgumentCaptor<FinanceVendor> captor = ArgumentCaptor.forClass(FinanceVendor.class);
        verify(financeVendorMapper).updateById(captor.capture());
        assertEquals("VEN001", captor.getValue().getCVenCode());
        assertEquals("COMPANY_A", captor.getValue().getCompanyId());
        assertEquals("creator", captor.getValue().getCCreatePerson());
        assertEquals("COMPANY_A", result.getCompanyId());
        assertEquals("New Vendor", result.getCVenName());
    }

    @Test
    void updateVendorPreservesCnapsWhenBranchSelectionIsUnchanged() {
        FinanceVendor existing = new FinanceVendor();
        existing.setCVenCode("VEN001");
        existing.setCVenName("Old Vendor");
        existing.setCompanyId("COMPANY_A");
        existing.setCVenBankCode("CMB");
        existing.setReceiptBankProvince("广东省");
        existing.setReceiptBankCity("深圳市");
        existing.setReceiptBranchCode("CMB-SZ-FH");
        existing.setReceiptBranchName("招商银行深圳福华支行");
        existing.setCVenBankNub("308584000013");

        FinanceVendor refreshed = new FinanceVendor();
        refreshed.setCVenCode("VEN001");
        refreshed.setCVenName("New Vendor");
        refreshed.setCompanyId("COMPANY_A");
        refreshed.setCVenBankNub("308584000013");

        FinanceVendorSaveDTO dto = new FinanceVendorSaveDTO();
        dto.setCVenName("New Vendor");
        dto.setCVenBankCode("CMB");
        dto.setReceiptBankProvince("广东省");
        dto.setReceiptBankCity("深圳市");
        dto.setReceiptBranchCode("CMB-SZ-FH");
        dto.setReceiptBranchName("招商银行深圳福华支行");

        when(financeVendorMapper.selectById("VEN001")).thenReturn(existing, refreshed);
        when(financeVendorMapper.updateById(any(FinanceVendor.class))).thenReturn(1);

        support.updateVendor("COMPANY_A", "VEN001", dto, "tester", false);

        ArgumentCaptor<FinanceVendor> captor = ArgumentCaptor.forClass(FinanceVendor.class);
        verify(financeVendorMapper).updateById(captor.capture());
        assertEquals("308584000013", captor.getValue().getCVenBankNub());
    }

    @Test
    void updateVendorClearsCnapsWhenBranchSelectionChanges() {
        FinanceVendor existing = new FinanceVendor();
        existing.setCVenCode("VEN001");
        existing.setCVenName("Old Vendor");
        existing.setCompanyId("COMPANY_A");
        existing.setCVenBankCode("CMB");
        existing.setReceiptBankProvince("广东省");
        existing.setReceiptBankCity("深圳市");
        existing.setReceiptBranchCode("CMB-SZ-FH");
        existing.setReceiptBranchName("招商银行深圳福华支行");
        existing.setCVenBankNub("308584000013");

        FinanceVendor refreshed = new FinanceVendor();
        refreshed.setCVenCode("VEN001");
        refreshed.setCVenName("New Vendor");
        refreshed.setCompanyId("COMPANY_A");

        FinanceVendorSaveDTO dto = new FinanceVendorSaveDTO();
        dto.setCVenName("New Vendor");
        dto.setCVenBankCode("CMB");
        dto.setReceiptBankProvince("广东省");
        dto.setReceiptBankCity("深圳市");
        dto.setReceiptBranchCode("CMB-SZ-NS");
        dto.setReceiptBranchName("招商银行深圳南山支行");

        when(financeVendorMapper.selectById("VEN001")).thenReturn(existing, refreshed);
        when(financeVendorMapper.updateById(any(FinanceVendor.class))).thenReturn(1);

        support.updateVendor("COMPANY_A", "VEN001", dto, "tester", false);

        ArgumentCaptor<FinanceVendor> captor = ArgumentCaptor.forClass(FinanceVendor.class);
        verify(financeVendorMapper).updateById(captor.capture());
        assertNull(captor.getValue().getCVenBankNub());
    }

    @Test
    void createVendorRejectsOverlongBankField() {
        FinanceVendorSaveDTO dto = new FinanceVendorSaveDTO();
        dto.setCVenName("Vendor A");
        dto.setCVenBank("B".repeat(129));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> support.createVendor("COMPANY_A", dto, "tester", false)
        );

        assertEquals("\u5f00\u6237\u94f6\u884c\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 128 \u4e2a\u5b57\u7b26", exception.getMessage());
    }

    @Test
    void disableVendorWritesSoftDeleteFields() {
        FinanceVendor existing = new FinanceVendor();
        existing.setCVenCode("VEN001");
        existing.setCompanyId("COMPANY_A");

        when(financeVendorMapper.selectById("VEN001")).thenReturn(existing);
        when(financeVendorMapper.updateById(any(FinanceVendor.class))).thenReturn(1);

        Boolean result = support.disableVendor("COMPANY_A", "VEN001", "tester");

        ArgumentCaptor<FinanceVendor> captor = ArgumentCaptor.forClass(FinanceVendor.class);
        verify(financeVendorMapper).updateById(captor.capture());
        assertNotNull(captor.getValue().getDEndDate());
        assertNotNull(captor.getValue().getUpdatedAt());
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void createVendorRequiresPaymentInfoWhenRequested() {
        FinanceVendorSaveDTO dto = new FinanceVendorSaveDTO();
        dto.setCVenName("Vendor A");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> support.createVendor("COMPANY_A", dto, "tester", true)
        );

        assertEquals("银行账号不能为空", exception.getMessage());
    }

    @Test
    void createVendorRejectsOverlongTightenedField() {
        FinanceVendorSaveDTO dto = new FinanceVendorSaveDTO();
        dto.setCVenName("Vendor A");
        dto.setCVenCode("V".repeat(65));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> support.createVendor("COMPANY_A", dto, "tester", false)
        );

        assertEquals("供应商编码长度不能超过 64 个字符", exception.getMessage());
    }
    @Test
    void createVendorUsesUnifiedBankDirectoryMessage() {
        FinanceVendorSaveDTO dto = new FinanceVendorSaveDTO();
        dto.setCVenName("Vendor A");
        dto.setCVenBank("招商银行");
        dto.setCVenBankCode("CMB");
        dto.setCVenAccount("622200001");
        dto.setReceiptBankProvince("广东省");
        dto.setReceiptBankCity("深圳市");
        dto.setReceiptBranchCode("CMB-SZ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> support.createVendor("COMPANY_A", dto, "tester", true)
        );

        assertEquals("请选择开户银行、开户省、开户市与开户网点后再保存", exception.getMessage());
    }
}
