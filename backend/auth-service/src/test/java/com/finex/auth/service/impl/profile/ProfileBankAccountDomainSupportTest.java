package com.finex.auth.service.impl.profile;

import com.finex.auth.dto.BankAccountVO;
import com.finex.auth.dto.UserBankAccountSaveDTO;
import com.finex.auth.entity.User;
import com.finex.auth.entity.UserBankAccount;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.service.UserService;
import com.finex.auth.service.impl.DownloadStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileBankAccountDomainSupportTest {

    @Mock
    private UserService userService;
    @Mock
    private UserBankAccountMapper userBankAccountMapper;
    @Mock
    private DownloadRecordMapper downloadRecordMapper;
    @Mock
    private DownloadStorageService downloadStorageService;

    private ProfileBankAccountDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new ProfileBankAccountDomainSupport(
                userService,
                userBankAccountMapper,
                downloadRecordMapper,
                downloadStorageService
        );
    }

    @Test
    void listBankAccountsMasksAccountNumberAndStatus() {
        User user = new User();
        user.setId(3L);
        when(userService.getById(3L)).thenReturn(user);

        UserBankAccount account = new UserBankAccount();
        account.setId(20L);
        account.setUserId(3L);
        account.setAccountName("Alice");
        account.setAccountNo("6222020202020202");
        account.setBankName("中国银行");
        account.setBranchName("上海分行");
        account.setProvince("上海");
        account.setCity("上海");
        account.setStatus(1);
        account.setDefaultAccount(1);

        when(userBankAccountMapper.selectList(any())).thenReturn(List.of(account));

        List<BankAccountVO> result = support.listBankAccounts(3L);

        assertEquals(1, result.size());
        assertEquals("6222 **** **** 0202", result.get(0).getAccountNoMasked());
        assertEquals("启用中", result.get(0).getStatusLabel());
        assertTrue(result.get(0).getDefaultAccount());
    }

    @Test
    void createBankAccountClearsOtherDefaultAccounts() {
        User user = new User();
        user.setId(3L);
        when(userService.getById(3L)).thenReturn(user);
        when(userBankAccountMapper.insert(any())).thenAnswer(invocation -> {
            UserBankAccount inserted = invocation.getArgument(0);
            inserted.setId(30L);
            return 1;
        });

        UserBankAccount existingDefault = new UserBankAccount();
        existingDefault.setId(11L);
        existingDefault.setUserId(3L);
        existingDefault.setDefaultAccount(1);

        UserBankAccount insertedAccount = new UserBankAccount();
        insertedAccount.setId(30L);
        insertedAccount.setUserId(3L);
        insertedAccount.setAccountName("New User");
        insertedAccount.setAccountNo("6222020202020202");
        insertedAccount.setBankName("招商银行");
        insertedAccount.setBranchName("深圳分行");
        insertedAccount.setProvince("广东");
        insertedAccount.setCity("深圳");
        insertedAccount.setStatus(1);
        insertedAccount.setDefaultAccount(1);

        when(userBankAccountMapper.selectList(any())).thenReturn(List.of(existingDefault));
        when(userBankAccountMapper.selectOne(any())).thenReturn(insertedAccount);

        UserBankAccountSaveDTO dto = new UserBankAccountSaveDTO();
        dto.setAccountName("New User");
        dto.setAccountNo("6222020202020202");
        dto.setBankCode("CMB");
        dto.setBankName("招商银行");
        dto.setProvince("广东");
        dto.setCity("深圳");
        dto.setBranchCode("CMB-SZ");
        dto.setBranchName("深圳分行");
        dto.setStatus(1);
        dto.setDefaultAccount(1);

        BankAccountVO result = support.createBankAccount(3L, dto);

        ArgumentCaptor<UserBankAccount> updateCaptor = ArgumentCaptor.forClass(UserBankAccount.class);
        verify(userBankAccountMapper).updateById(updateCaptor.capture());
        assertEquals(11L, updateCaptor.getValue().getId());
        assertEquals(0, updateCaptor.getValue().getDefaultAccount());
        assertEquals(30L, result.getId());
    }

    @Test
    void setDefaultBankAccountRejectsDisabledAccount() {
        User user = new User();
        user.setId(3L);
        when(userService.getById(3L)).thenReturn(user);

        UserBankAccount disabled = new UserBankAccount();
        disabled.setId(40L);
        disabled.setUserId(3L);
        disabled.setStatus(0);

        when(userBankAccountMapper.selectOne(any())).thenReturn(disabled);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> support.setDefaultBankAccount(3L, 40L)
        );

        assertEquals("停用账户不能设为默认", error.getMessage());
        verify(userBankAccountMapper, times(0)).updateById(any());
    }

    @Test
    void updateBankAccountPreservesCnapsWhenBranchSelectionIsUnchanged() {
        User user = new User();
        user.setId(3L);
        when(userService.getById(3L)).thenReturn(user);

        UserBankAccount existing = new UserBankAccount();
        existing.setId(20L);
        existing.setUserId(3L);
        existing.setBankCode("CMB");
        existing.setBankName("招商银行");
        existing.setProvince("广东省");
        existing.setCity("深圳市");
        existing.setBranchCode("CMB-SZ-FH");
        existing.setBranchName("招商银行深圳福华支行");
        existing.setCnapsCode("308584000013");
        existing.setStatus(1);

        UserBankAccount refreshed = new UserBankAccount();
        refreshed.setId(20L);
        refreshed.setUserId(3L);

        when(userBankAccountMapper.selectOne(any())).thenReturn(existing, refreshed);

        UserBankAccountSaveDTO dto = new UserBankAccountSaveDTO();
        dto.setAccountName("Alice");
        dto.setAccountNo("6222020202020202");
        dto.setBankCode("CMB");
        dto.setBankName("招商银行");
        dto.setProvince("广东省");
        dto.setCity("深圳市");
        dto.setBranchCode("CMB-SZ-FH");
        dto.setBranchName("招商银行深圳福华支行");
        dto.setStatus(1);
        dto.setDefaultAccount(0);

        support.updateBankAccount(3L, 20L, dto);

        ArgumentCaptor<UserBankAccount> captor = ArgumentCaptor.forClass(UserBankAccount.class);
        verify(userBankAccountMapper).updateById(captor.capture());
        assertEquals("308584000013", captor.getValue().getCnapsCode());
    }

    @Test
    void updateBankAccountClearsCnapsWhenBranchSelectionChanges() {
        User user = new User();
        user.setId(3L);
        when(userService.getById(3L)).thenReturn(user);

        UserBankAccount existing = new UserBankAccount();
        existing.setId(21L);
        existing.setUserId(3L);
        existing.setBankCode("CMB");
        existing.setBankName("招商银行");
        existing.setProvince("广东省");
        existing.setCity("深圳市");
        existing.setBranchCode("CMB-SZ-FH");
        existing.setBranchName("招商银行深圳福华支行");
        existing.setCnapsCode("308584000013");
        existing.setStatus(1);

        UserBankAccount refreshed = new UserBankAccount();
        refreshed.setId(21L);
        refreshed.setUserId(3L);

        when(userBankAccountMapper.selectOne(any())).thenReturn(existing, refreshed);

        UserBankAccountSaveDTO dto = new UserBankAccountSaveDTO();
        dto.setAccountName("Alice");
        dto.setAccountNo("6222020202020202");
        dto.setBankCode("CMB");
        dto.setBankName("招商银行");
        dto.setProvince("广东省");
        dto.setCity("深圳市");
        dto.setBranchCode("CMB-SZ-NS");
        dto.setBranchName("招商银行深圳南山支行");
        dto.setStatus(1);
        dto.setDefaultAccount(0);

        support.updateBankAccount(3L, 21L, dto);

        ArgumentCaptor<UserBankAccount> captor = ArgumentCaptor.forClass(UserBankAccount.class);
        verify(userBankAccountMapper).updateById(captor.capture());
        assertNull(captor.getValue().getCnapsCode());
    }
    @Test
    void createBankAccountUsesUnifiedOutletValidationMessage() {
        User user = new User();
        user.setId(3L);
        when(userService.getById(3L)).thenReturn(user);

        UserBankAccountSaveDTO dto = new UserBankAccountSaveDTO();
        dto.setAccountName("Alice");
        dto.setAccountNo("6222020202020202");
        dto.setBankCode("CMB");
        dto.setBankName("招商银行");
        dto.setProvince("广东省");
        dto.setCity("深圳市");
        dto.setBranchCode("CMB-SZ");
        dto.setStatus(1);
        dto.setDefaultAccount(0);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> support.createBankAccount(3L, dto)
        );

        assertEquals("\u5f00\u6237\u7f51\u70b9\u4e0d\u80fd\u4e3a\u7a7a", exception.getMessage());
    }
}
