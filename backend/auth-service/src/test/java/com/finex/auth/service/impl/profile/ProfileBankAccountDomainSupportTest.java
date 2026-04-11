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
        dto.setBankName("招商银行");
        dto.setProvince("广东");
        dto.setCity("深圳");
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
}
