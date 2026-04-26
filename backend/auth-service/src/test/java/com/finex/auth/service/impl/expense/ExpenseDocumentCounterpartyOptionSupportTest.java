package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.User;
import com.finex.auth.entity.UserBankAccount;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.FinanceVendorService;
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
class ExpenseDocumentCounterpartyOptionSupportTest {

    @Mock private FinanceVendorMapper financeVendorMapper;
    @Mock private UserMapper userMapper;
    @Mock private UserBankAccountMapper userBankAccountMapper;
    @Mock private FinanceVendorService financeVendorService;

    @Test
    void listVendorOptionsUsesResolvedCompanyId() {
        ExpenseDocumentCounterpartyOptionSupport support = newSupport();
        User user = new User();
        user.setId(1L);
        user.setStatus(1);
        user.setCompanyId("COMPANY_A");
        List<ExpenseCreateVendorOptionVO> expected = List.of(new ExpenseCreateVendorOptionVO());
        when(userMapper.selectById(1L)).thenReturn(user);
        when(financeVendorService.listActiveVendorOptions("COMPANY_A", "abc", false)).thenReturn(expected);

        List<ExpenseCreateVendorOptionVO> actual = support.listVendorOptions(1L, "abc", false, null);

        assertEquals(expected, actual);
        verify(financeVendorService).listActiveVendorOptions("COMPANY_A", "abc", false);
    }

    @Test
    void listPayeeOptionsInPersonalModeDeduplicatesByAccountName() {
        ExpenseDocumentCounterpartyOptionSupport support = newSupport();
        UserBankAccount first = new UserBankAccount();
        first.setId(1L);
        first.setUserId(9L);
        first.setStatus(1);
        first.setAccountName("张三");
        first.setAccountNo("6222333344445555");
        first.setBankName("招行");
        UserBankAccount second = new UserBankAccount();
        second.setId(2L);
        second.setUserId(9L);
        second.setStatus(1);
        second.setAccountName("张三");
        second.setAccountNo("6222666677778888");
        second.setBankName("工行");
        when(userBankAccountMapper.selectList(any())).thenReturn(List.of(first, second));

        List<ExpenseCreatePayeeOptionVO> actual = support.listPayeeOptions(9L, "张", true);

        assertEquals(1, actual.size());
        assertEquals("PERSONAL_PAYEE:张三", actual.get(0).getValue());
        assertEquals("PERSONAL_PRIVATE_PAYEE", actual.get(0).getSourceType());
    }

    @Test
    void listPayeeAccountOptionsInEnterpriseModeReturnsVendorAccount() {
        ExpenseDocumentCounterpartyOptionSupport support = newSupport();
        FinanceVendor vendor = new FinanceVendor();
        vendor.setCompanyId("COMPANY_A");
        vendor.setCVenCode("VEN-1");
        vendor.setCVenName("供应商A");
        vendor.setReceiptAccountName("供应商A");
        vendor.setCVenBank("中国银行");
        vendor.setCVenAccount("6222020202020202");
        vendor.setCVenBankNub("朝阳支行");
        vendor.setReceiptBranchName("朝阳支行");
        when(financeVendorMapper.selectOne(any())).thenReturn(vendor);

        List<ExpenseCreatePayeeAccountOptionVO> actual = support.listPayeeAccountOptions(
                1L,
                "供应商",
                "ENTERPRISE",
                null,
                "VEN-1",
                "COMPANY_A"
        );

        assertEquals(1, actual.size());
        ExpenseCreatePayeeAccountOptionVO option = actual.get(0);
        assertEquals("VENDOR:VEN-1", option.getValue());
        assertEquals("VENDOR", option.getSourceType());
        assertEquals("6222 **** 0202", option.getAccountNoMasked());
        assertEquals("朝阳支行 / 6222 **** 0202", option.getSecondaryLabel());
    }

    @Test
    void listPayeeOptionsRejectsUserWithoutCompanyBinding() {
        ExpenseDocumentCounterpartyOptionSupport support = newSupport();
        User user = new User();
        user.setId(1L);
        user.setStatus(1);
        when(userMapper.selectById(1L)).thenReturn(user);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> support.listPayeeOptions(1L, null, false)
        );

        assertEquals("当前用户未配置所属公司，无法继续处理", error.getMessage());
    }

    private ExpenseDocumentCounterpartyOptionSupport newSupport() {
        return new ExpenseDocumentCounterpartyOptionSupport(
                financeVendorMapper,
                userMapper,
                userBankAccountMapper,
                financeVendorService
        );
    }
}
