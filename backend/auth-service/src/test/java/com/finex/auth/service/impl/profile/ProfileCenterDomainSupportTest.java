package com.finex.auth.service.impl.profile;

import cn.hutool.crypto.digest.DigestUtil;
import com.finex.auth.dto.ChangePasswordDTO;
import com.finex.auth.dto.PersonalCenterVO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileCenterDomainSupportTest {

    @Mock
    private UserService userService;
    @Mock
    private UserBankAccountMapper userBankAccountMapper;
    @Mock
    private DownloadRecordMapper downloadRecordMapper;
    @Mock
    private DownloadStorageService downloadStorageService;

    private ProfileCenterDomainSupport support;

    @BeforeEach
    void setUp() {
        ProfileBankAccountDomainSupport bankSupport = new ProfileBankAccountDomainSupport(
                userService,
                userBankAccountMapper,
                downloadRecordMapper,
                downloadStorageService
        );
        support = new ProfileCenterDomainSupport(
                userService,
                userBankAccountMapper,
                downloadRecordMapper,
                downloadStorageService,
                bankSupport
        );
    }

    @Test
    void getPersonalCenterBuildsProfileAndBankAccounts() {
        User user = new User();
        user.setId(2L);
        user.setUsername("bob");
        user.setPosition(null);
        user.setLaborRelationBelong(null);

        UserBankAccount account = new UserBankAccount();
        account.setId(9L);
        account.setUserId(2L);
        account.setAccountName("Bob");
        account.setAccountNo("6222020202020202");
        account.setBankName("招商银行");
        account.setBranchName("深圳分行");
        account.setProvince("广东");
        account.setCity("深圳");
        account.setStatus(1);

        when(userService.getById(2L)).thenReturn(user);
        when(userService.getRoleCodes(2L)).thenReturn(List.of("USER"));
        when(userService.getPermissionCodes(2L)).thenReturn(List.of("profile:view"));
        when(userBankAccountMapper.selectList(any())).thenReturn(List.of(account));

        PersonalCenterVO result = support.getPersonalCenter(2L);

        assertEquals("bob", result.getUser().getUsername());
        assertEquals("员工", result.getUser().getPosition());
        assertEquals("总部", result.getUser().getLaborRelationBelong());
        assertEquals(1, result.getBankAccounts().size());
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        User user = new User();
        user.setId(2L);
        user.setPassword(DigestUtil.md5Hex("old-password"));
        when(userService.getById(2L)).thenReturn(user);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setCurrentPassword("bad-password");
        dto.setNewPassword("new-password");
        dto.setConfirmPassword("new-password");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> support.changePassword(2L, dto)
        );

        assertEquals("当前密码不正确", error.getMessage());
    }

    @Test
    void changePasswordHashesAndPersistsNewPassword() {
        User user = new User();
        user.setId(2L);
        user.setPassword(DigestUtil.md5Hex("old-password"));
        when(userService.getById(2L)).thenReturn(user);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setCurrentPassword("old-password");
        dto.setNewPassword("new-password");
        dto.setConfirmPassword("new-password");

        support.changePassword(2L, dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(captor.capture());
        assertEquals(DigestUtil.md5Hex("new-password"), captor.getValue().getPassword());
    }
}
