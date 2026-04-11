package com.finex.auth.service.impl.profile;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.finex.auth.dto.ChangePasswordDTO;
import com.finex.auth.dto.PersonalCenterVO;
import com.finex.auth.dto.UserProfileVO;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.service.UserService;
import com.finex.auth.service.impl.DownloadStorageService;

public final class ProfileCenterDomainSupport extends AbstractProfileDomainSupport {

    private final ProfileBankAccountDomainSupport profileBankAccountDomainSupport;

    public ProfileCenterDomainSupport(
            UserService userService,
            UserBankAccountMapper userBankAccountMapper,
            DownloadRecordMapper downloadRecordMapper,
            DownloadStorageService downloadStorageService,
            ProfileBankAccountDomainSupport profileBankAccountDomainSupport
    ) {
        super(userService, userBankAccountMapper, downloadRecordMapper, downloadStorageService);
        this.profileBankAccountDomainSupport = profileBankAccountDomainSupport;
    }

    public PersonalCenterVO getPersonalCenter(Long userId) {
        User user = requireUser(userId);

        PersonalCenterVO center = new PersonalCenterVO();
        center.setUser(toUserProfile(user));
        center.setBankAccounts(profileBankAccountDomainSupport.loadBankAccounts(userId));
        return center;
    }

    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = requireUser(userId);
        String currentPassword = DigestUtil.md5Hex(dto.getCurrentPassword());
        if (!currentPassword.equals(user.getPassword())) {
            throw new IllegalArgumentException("当前密码不正确");
        }
        if (!StrUtil.equals(dto.getNewPassword(), dto.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的新密码不一致");
        }
        if (dto.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("新密码长度不能少于 6 位");
        }

        user.setPassword(DigestUtil.md5Hex(dto.getNewPassword()));
        userService().updateById(user);
    }

    private UserProfileVO toUserProfile(User user) {
        UserProfileVO profile = new UserProfileVO();
        profile.setUserId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setName(blankToDefault(user.getName(), user.getUsername()));
        profile.setPhone(user.getPhone());
        profile.setEmail(user.getEmail());
        profile.setPosition(blankToDefault(user.getPosition(), "员工"));
        profile.setLaborRelationBelong(blankToDefault(user.getLaborRelationBelong(), "总部"));
        profile.setCompanyId(user.getCompanyId());
        profile.setRoles(userService().getRoleCodes(user.getId()));
        profile.setPermissionCodes(userService().getPermissionCodes(user.getId()));
        return profile;
    }
}
