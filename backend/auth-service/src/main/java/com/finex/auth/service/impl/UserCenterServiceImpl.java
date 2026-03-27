package com.finex.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.BankAccountVO;
import com.finex.auth.dto.ChangePasswordDTO;
import com.finex.auth.dto.DownloadCenterVO;
import com.finex.auth.dto.DownloadRecordVO;
import com.finex.auth.dto.PersonalCenterVO;
import com.finex.auth.dto.UserProfileVO;
import com.finex.auth.entity.DownloadRecord;
import com.finex.auth.entity.User;
import com.finex.auth.entity.UserBankAccount;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.service.UserCenterService;
import com.finex.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCenterServiceImpl implements UserCenterService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserService userService;
    private final UserBankAccountMapper userBankAccountMapper;
    private final DownloadRecordMapper downloadRecordMapper;

    @Override
    public PersonalCenterVO getPersonalCenter(Long userId) {
        User user = requireUser(userId);

        PersonalCenterVO center = new PersonalCenterVO();
        center.setUser(toUserProfile(user));
        center.setBankAccounts(listBankAccounts(userId));
        return center;
    }

    @Override
    public DownloadCenterVO getDownloadCenter(Long userId) {
        List<DownloadRecord> records = downloadRecordMapper.selectList(
                Wrappers.<DownloadRecord>lambdaQuery()
                        .eq(DownloadRecord::getUserId, userId)
                        .orderByDesc(DownloadRecord::getCreatedAt, DownloadRecord::getId)
                        .last("limit 20")
        );

        DownloadCenterVO center = new DownloadCenterVO();
        center.setInProgress(records.stream()
                .filter(item -> "DOWNLOADING".equalsIgnoreCase(item.getStatus()))
                .map(this::toDownloadRecord)
                .toList());
        center.setHistory(records.stream()
                .filter(item -> !"DOWNLOADING".equalsIgnoreCase(item.getStatus()))
                .map(this::toDownloadRecord)
                .toList());
        return center;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
        userService.updateById(user);
    }

    private List<BankAccountVO> listBankAccounts(Long userId) {
        return userBankAccountMapper.selectList(
                Wrappers.<UserBankAccount>lambdaQuery()
                        .eq(UserBankAccount::getUserId, userId)
                        .orderByDesc(UserBankAccount::getDefaultAccount)
                        .orderByAsc(UserBankAccount::getId)
        ).stream().map(this::toBankAccount).toList();
    }

    private User requireUser(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("当前用户不存在");
        }
        return user;
    }

    private UserProfileVO toUserProfile(User user) {
        UserProfileVO profile = new UserProfileVO();
        profile.setUserId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setName(StrUtil.blankToDefault(user.getName(), user.getUsername()));
        profile.setPhone(user.getPhone());
        profile.setEmail(user.getEmail());
        profile.setPosition(StrUtil.blankToDefault(user.getPosition(), "员工"));
        profile.setLaborRelationBelong(StrUtil.blankToDefault(user.getLaborRelationBelong(), "总部"));
        profile.setCompanyId(user.getCompanyId());
        profile.setRoles(userService.getRoleCodes(user.getId()));
        profile.setPermissionCodes(userService.getPermissionCodes(user.getId()));
        return profile;
    }

    private BankAccountVO toBankAccount(UserBankAccount account) {
        BankAccountVO vo = new BankAccountVO();
        vo.setId(account.getId());
        vo.setBankName(account.getBankName());
        vo.setBranchName(account.getBranchName());
        vo.setAccountName(account.getAccountName());
        vo.setAccountNoMasked(maskAccountNo(account.getAccountNo()));
        vo.setAccountType(StrUtil.blankToDefault(account.getAccountType(), "对私账户"));
        vo.setDefaultAccount(Integer.valueOf(1).equals(account.getDefaultAccount()));
        vo.setStatus(Integer.valueOf(1).equals(account.getStatus()) ? "启用中" : "已停用");
        return vo;
    }

    private DownloadRecordVO toDownloadRecord(DownloadRecord record) {
        DownloadRecordVO vo = new DownloadRecordVO();
        vo.setId(record.getId());
        vo.setFileName(record.getFileName());
        vo.setBusinessType(record.getBusinessType());
        vo.setStatus(resolveDownloadStatus(record.getStatus()));
        vo.setProgress(record.getProgress() == null ? 0 : record.getProgress());
        vo.setFileSize(StrUtil.blankToDefault(record.getFileSize(), "-"));
        vo.setCreatedAt(record.getCreatedAt() == null ? "" : record.getCreatedAt().format(DATE_TIME_FORMATTER));
        vo.setFinishedAt(record.getFinishedAt() == null ? "" : record.getFinishedAt().format(DATE_TIME_FORMATTER));
        return vo;
    }

    private String resolveDownloadStatus(String status) {
        if ("DOWNLOADING".equalsIgnoreCase(status)) {
            return "下载中";
        }
        if ("FAILED".equalsIgnoreCase(status)) {
            return "失败";
        }
        return "已完成";
    }

    private String maskAccountNo(String accountNo) {
        if (accountNo == null || accountNo.length() < 8) {
            return StrUtil.blankToDefault(accountNo, "-");
        }
        return accountNo.substring(0, 4) + " **** **** " + accountNo.substring(accountNo.length() - 4);
    }
}
