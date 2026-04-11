package com.finex.auth.service.impl;

import com.finex.auth.dto.BankAccountVO;
import com.finex.auth.dto.ChangePasswordDTO;
import com.finex.auth.dto.DownloadCenterVO;
import com.finex.auth.dto.PersonalCenterVO;
import com.finex.auth.dto.UserBankAccountSaveDTO;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.service.UserCenterService;
import com.finex.auth.service.UserService;
import com.finex.auth.service.impl.profile.ProfileBankAccountDomainSupport;
import com.finex.auth.service.impl.profile.ProfileCenterDomainSupport;
import com.finex.auth.service.impl.profile.ProfileDownloadDomainSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserCenterServiceImpl implements UserCenterService {

    private final ProfileCenterDomainSupport profileCenterDomainSupport;
    private final ProfileBankAccountDomainSupport profileBankAccountDomainSupport;
    private final ProfileDownloadDomainSupport profileDownloadDomainSupport;

    public UserCenterServiceImpl(
            UserService userService,
            UserBankAccountMapper userBankAccountMapper,
            DownloadRecordMapper downloadRecordMapper,
            DownloadStorageService downloadStorageService
    ) {
        this.profileBankAccountDomainSupport = new ProfileBankAccountDomainSupport(
                userService,
                userBankAccountMapper,
                downloadRecordMapper,
                downloadStorageService
        );
        this.profileCenterDomainSupport = new ProfileCenterDomainSupport(
                userService,
                userBankAccountMapper,
                downloadRecordMapper,
                downloadStorageService,
                profileBankAccountDomainSupport
        );
        this.profileDownloadDomainSupport = new ProfileDownloadDomainSupport(
                userService,
                userBankAccountMapper,
                downloadRecordMapper,
                downloadStorageService
        );
    }

    @Override
    public PersonalCenterVO getPersonalCenter(Long userId) {
        return profileCenterDomainSupport.getPersonalCenter(userId);
    }

    @Override
    public List<BankAccountVO> listBankAccounts(Long userId) {
        return profileBankAccountDomainSupport.listBankAccounts(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankAccountVO createBankAccount(Long userId, UserBankAccountSaveDTO dto) {
        return profileBankAccountDomainSupport.createBankAccount(userId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankAccountVO updateBankAccount(Long userId, Long accountId, UserBankAccountSaveDTO dto) {
        return profileBankAccountDomainSupport.updateBankAccount(userId, accountId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateBankAccountStatus(Long userId, Long accountId, Integer status) {
        return profileBankAccountDomainSupport.updateBankAccountStatus(userId, accountId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean setDefaultBankAccount(Long userId, Long accountId) {
        return profileBankAccountDomainSupport.setDefaultBankAccount(userId, accountId);
    }

    @Override
    public DownloadCenterVO getDownloadCenter(Long userId) {
        return profileDownloadDomainSupport.getDownloadCenter(userId);
    }

    @Override
    public DownloadContent loadDownloadContent(Long userId, Long downloadId) {
        return profileDownloadDomainSupport.loadDownloadContent(userId, downloadId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        profileCenterDomainSupport.changePassword(userId, dto);
    }
}
