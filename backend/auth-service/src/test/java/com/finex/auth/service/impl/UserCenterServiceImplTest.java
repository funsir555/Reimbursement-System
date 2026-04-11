package com.finex.auth.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.finex.auth.dto.ChangePasswordDTO;
import com.finex.auth.dto.DownloadCenterVO;
import com.finex.auth.dto.PersonalCenterVO;
import com.finex.auth.entity.DownloadRecord;
import com.finex.auth.entity.User;
import com.finex.auth.entity.UserBankAccount;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.service.UserService;
import com.finex.auth.support.AsyncTaskSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCenterServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private UserBankAccountMapper userBankAccountMapper;
    @Mock
    private DownloadRecordMapper downloadRecordMapper;
    @Mock
    private DownloadStorageService downloadStorageService;

    private UserCenterServiceImpl userCenterService;

    @BeforeEach
    void setUp() {
        userCenterService = new UserCenterServiceImpl(
                userService,
                userBankAccountMapper,
                downloadRecordMapper,
                downloadStorageService
        );
    }

    @Test
    void getPersonalCenterDelegatesThroughFacade() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setName("Alice");
        user.setPosition("财务");
        user.setLaborRelationBelong("总部");

        UserBankAccount account = new UserBankAccount();
        account.setId(8L);
        account.setUserId(1L);
        account.setAccountName("Alice");
        account.setAccountNo("6222020202020202");
        account.setBankName("中国银行");
        account.setBranchName("上海分行");
        account.setProvince("上海");
        account.setCity("上海");
        account.setStatus(1);
        account.setDefaultAccount(1);

        when(userService.getById(1L)).thenReturn(user);
        when(userService.getRoleCodes(1L)).thenReturn(List.of("EMPLOYEE"));
        when(userService.getPermissionCodes(1L)).thenReturn(List.of("profile:view"));
        when(userBankAccountMapper.selectList(any())).thenReturn(List.of(account));

        PersonalCenterVO result = userCenterService.getPersonalCenter(1L);

        assertEquals("alice", result.getUser().getUsername());
        assertEquals(1, result.getBankAccounts().size());
        assertTrue(result.getBankAccounts().get(0).getDefaultAccount());
        assertEquals("6222 **** **** 0202", result.getBankAccounts().get(0).getAccountNoMasked());
    }

    @Test
    void getDownloadCenterDelegatesThroughFacade() {
        DownloadRecord downloading = new DownloadRecord();
        downloading.setId(11L);
        downloading.setUserId(1L);
        downloading.setFileName("exporting.xlsx");
        downloading.setStatus(AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING);
        downloading.setProgress(10);
        downloading.setCreatedAt(LocalDateTime.of(2026, 4, 10, 12, 0, 0));

        DownloadRecord completed = new DownloadRecord();
        completed.setId(12L);
        completed.setUserId(1L);
        completed.setFileName("finished.xlsx");
        completed.setStatus(AsyncTaskSupport.DOWNLOAD_STATUS_COMPLETED);
        completed.setProgress(100);
        completed.setCreatedAt(LocalDateTime.of(2026, 4, 10, 11, 0, 0));
        completed.setFinishedAt(LocalDateTime.of(2026, 4, 10, 11, 5, 0));

        when(downloadRecordMapper.selectList(any())).thenReturn(List.of(downloading, completed));
        when(downloadStorageService.exists(12L)).thenReturn(true);

        DownloadCenterVO result = userCenterService.getDownloadCenter(1L);

        assertEquals(1, result.getInProgress().size());
        assertEquals(1, result.getHistory().size());
        assertEquals("/auth/user-center/downloads/12/content", result.getHistory().get(0).getDownloadUrl());
    }

    @Test
    void changePasswordDelegatesThroughFacade() {
        User user = new User();
        user.setId(1L);
        user.setPassword(DigestUtil.md5Hex("old-password"));
        when(userService.getById(1L)).thenReturn(user);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setCurrentPassword("old-password");
        dto.setNewPassword("new-password");
        dto.setConfirmPassword("new-password");

        userCenterService.changePassword(1L, dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(captor.capture());
        assertEquals(DigestUtil.md5Hex("new-password"), captor.getValue().getPassword());
    }
}
