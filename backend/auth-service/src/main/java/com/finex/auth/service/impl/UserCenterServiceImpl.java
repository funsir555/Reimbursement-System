package com.finex.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.BankAccountVO;
import com.finex.auth.dto.ChangePasswordDTO;
import com.finex.auth.dto.DownloadCenterVO;
import com.finex.auth.dto.DownloadRecordVO;
import com.finex.auth.dto.PersonalCenterVO;
import com.finex.auth.dto.UserBankAccountSaveDTO;
import com.finex.auth.dto.UserProfileVO;
import com.finex.auth.entity.DownloadRecord;
import com.finex.auth.entity.User;
import com.finex.auth.entity.UserBankAccount;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.service.UserCenterService;
import com.finex.auth.service.UserService;
import com.finex.auth.support.AsyncTaskSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserCenterServiceImpl implements UserCenterService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserService userService;
    private final UserBankAccountMapper userBankAccountMapper;
    private final DownloadRecordMapper downloadRecordMapper;
    private final DownloadStorageService downloadStorageService;

    @Override
    public PersonalCenterVO getPersonalCenter(Long userId) {
        User user = requireUser(userId);

        PersonalCenterVO center = new PersonalCenterVO();
        center.setUser(toUserProfile(user));
        center.setBankAccounts(loadBankAccounts(userId));
        return center;
    }

    @Override
    public List<BankAccountVO> listBankAccounts(Long userId) {
        requireUser(userId);
        return loadBankAccounts(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankAccountVO createBankAccount(Long userId, UserBankAccountSaveDTO dto) {
        requireUser(userId);
        UserBankAccount account = new UserBankAccount();
        applyBankAccount(account, dto);
        account.setUserId(userId);
        userBankAccountMapper.insert(account);
        if (Integer.valueOf(1).equals(account.getDefaultAccount())) {
            clearOtherDefaultBankAccounts(userId, account.getId());
        }
        return findBankAccount(userId, account.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankAccountVO updateBankAccount(Long userId, Long accountId, UserBankAccountSaveDTO dto) {
        requireUser(userId);
        UserBankAccount account = requireBankAccount(userId, accountId);
        applyBankAccount(account, dto);
        userBankAccountMapper.updateById(account);
        if (Integer.valueOf(1).equals(account.getDefaultAccount())) {
            clearOtherDefaultBankAccounts(userId, accountId);
        }
        return findBankAccount(userId, accountId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateBankAccountStatus(Long userId, Long accountId, Integer status) {
        requireUser(userId);
        UserBankAccount account = requireBankAccount(userId, accountId);
        account.setStatus(normalizeStatus(status));
        if (!Integer.valueOf(1).equals(account.getStatus())) {
            account.setDefaultAccount(0);
        }
        userBankAccountMapper.updateById(account);
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean setDefaultBankAccount(Long userId, Long accountId) {
        requireUser(userId);
        UserBankAccount account = requireBankAccount(userId, accountId);
        if (!Integer.valueOf(1).equals(account.getStatus())) {
            throw new IllegalArgumentException("停用账户不能设为默认");
        }
        account.setDefaultAccount(1);
        userBankAccountMapper.updateById(account);
        clearOtherDefaultBankAccounts(userId, accountId);
        return Boolean.TRUE;
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
                .filter(this::isDownloading)
                .map(this::toDownloadRecord)
                .toList());
        center.setHistory(records.stream()
                .filter(record -> !isDownloading(record))
                .map(this::toDownloadRecord)
                .toList());
        return center;
    }

    @Override
    public DownloadContent loadDownloadContent(Long userId, Long downloadId) {
        DownloadRecord record = requireDownloadRecord(userId, downloadId);
        if (!AsyncTaskSupport.DOWNLOAD_STATUS_COMPLETED.equalsIgnoreCase(record.getStatus())) {
            throw new IllegalStateException("下载文件尚未生成完成");
        }
        Path path = downloadStorageService.resolvePath(record.getId());
        if (!Files.exists(path)) {
            throw new IllegalStateException("下载文件不存在或已失效");
        }
        Resource resource = new FileSystemResource(path);
        try {
            return new DownloadContent(resource, record.getFileName(), Files.size(path));
        } catch (Exception ex) {
            throw new IllegalStateException("读取下载文件失败", ex);
        }
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

    private List<BankAccountVO> loadBankAccounts(Long userId) {
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

    private DownloadRecord requireDownloadRecord(Long userId, Long downloadId) {
        DownloadRecord record = downloadRecordMapper.selectOne(
                Wrappers.<DownloadRecord>lambdaQuery()
                        .eq(DownloadRecord::getId, downloadId)
                        .eq(DownloadRecord::getUserId, userId)
                        .last("limit 1")
        );
        if (record == null) {
            throw new IllegalArgumentException("下载记录不存在");
        }
        return record;
    }

    private UserBankAccount requireBankAccount(Long userId, Long accountId) {
        UserBankAccount account = userBankAccountMapper.selectOne(
                Wrappers.<UserBankAccount>lambdaQuery()
                        .eq(UserBankAccount::getId, accountId)
                        .eq(UserBankAccount::getUserId, userId)
                        .last("limit 1")
        );
        if (account == null) {
            throw new IllegalArgumentException("收款账户不存在");
        }
        return account;
    }

    private BankAccountVO findBankAccount(Long userId, Long accountId) {
        return toBankAccount(requireBankAccount(userId, accountId));
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
        vo.setBankCode(account.getBankCode());
        vo.setBankName(account.getBankName());
        vo.setProvince(account.getProvince());
        vo.setCity(account.getCity());
        vo.setBranchCode(account.getBranchCode());
        vo.setBranchName(account.getBranchName());
        vo.setCnapsCode(account.getCnapsCode());
        vo.setAccountName(account.getAccountName());
        vo.setAccountNo(account.getAccountNo());
        vo.setAccountNoMasked(maskAccountNo(account.getAccountNo()));
        vo.setAccountType(StrUtil.blankToDefault(account.getAccountType(), "对私账户"));
        vo.setDefaultAccount(Integer.valueOf(1).equals(account.getDefaultAccount()));
        vo.setStatus(normalizeStatus(account.getStatus()));
        vo.setStatusLabel(Integer.valueOf(1).equals(vo.getStatus()) ? "启用中" : "已停用");
        vo.setCreatedAt(account.getCreatedAt() == null ? "" : account.getCreatedAt().format(DATE_TIME_FORMATTER));
        vo.setUpdatedAt(account.getUpdatedAt() == null ? "" : account.getUpdatedAt().format(DATE_TIME_FORMATTER));
        return vo;
    }

    private void applyBankAccount(UserBankAccount account, UserBankAccountSaveDTO dto) {
        account.setAccountName(requireText(dto.getAccountName(), "账户名不能为空"));
        account.setAccountNo(requireText(dto.getAccountNo(), "银行账号不能为空"));
        account.setAccountType(defaultText(trimToNull(dto.getAccountType()), "对私账户"));
        account.setBankCode(trimToNull(dto.getBankCode()));
        account.setBankName(requireText(dto.getBankName(), "开户银行不能为空"));
        account.setProvince(requireText(dto.getProvince(), "开户省不能为空"));
        account.setCity(requireText(dto.getCity(), "开户市不能为空"));
        account.setBranchCode(trimToNull(dto.getBranchCode()));
        account.setBranchName(requireText(dto.getBranchName(), "分支行不能为空"));
        account.setCnapsCode(trimToNull(dto.getCnapsCode()));
        account.setStatus(normalizeStatus(dto.getStatus()));
        account.setDefaultAccount(Integer.valueOf(1).equals(account.getStatus()) && normalizeFlag(dto.getDefaultAccount()) == 1 ? 1 : 0);
    }

    private void clearOtherDefaultBankAccounts(Long userId, Long currentId) {
        List<UserBankAccount> accounts = userBankAccountMapper.selectList(
                Wrappers.<UserBankAccount>lambdaQuery()
                        .eq(UserBankAccount::getUserId, userId)
                        .eq(UserBankAccount::getDefaultAccount, 1)
        );
        for (UserBankAccount account : accounts) {
            if (Objects.equals(account.getId(), currentId)) {
                continue;
            }
            account.setDefaultAccount(0);
            userBankAccountMapper.updateById(account);
        }
    }

    private Integer normalizeStatus(Integer status) {
        return Integer.valueOf(0).equals(status) ? 0 : 1;
    }

    private int normalizeFlag(Integer value) {
        return Integer.valueOf(1).equals(value) ? 1 : 0;
    }

    private String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String defaultText(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private DownloadRecordVO toDownloadRecord(DownloadRecord record) {
        boolean downloadable = isDownloadable(record);
        DownloadRecordVO vo = new DownloadRecordVO();
        vo.setId(record.getId());
        vo.setFileName(record.getFileName());
        vo.setBusinessType(record.getBusinessType());
        vo.setStatus(resolveDownloadStatus(record.getStatus()));
        vo.setProgress(record.getProgress() == null ? 0 : record.getProgress());
        vo.setFileSize(StrUtil.blankToDefault(record.getFileSize(), "-"));
        vo.setCreatedAt(record.getCreatedAt() == null ? "" : record.getCreatedAt().format(DATE_TIME_FORMATTER));
        vo.setFinishedAt(record.getFinishedAt() == null ? "" : record.getFinishedAt().format(DATE_TIME_FORMATTER));
        vo.setDownloadable(downloadable);
        vo.setDownloadUrl(downloadable ? "/auth/user-center/downloads/" + record.getId() + "/content" : null);
        return vo;
    }

    private boolean isDownloading(DownloadRecord record) {
        return AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING.equalsIgnoreCase(record.getStatus());
    }

    private boolean isDownloadable(DownloadRecord record) {
        return AsyncTaskSupport.DOWNLOAD_STATUS_COMPLETED.equalsIgnoreCase(record.getStatus())
                && downloadStorageService.exists(record.getId());
    }

    private String resolveDownloadStatus(String status) {
        if (AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING.equalsIgnoreCase(status)) {
            return "下载中";
        }
        if (AsyncTaskSupport.DOWNLOAD_STATUS_FAILED.equalsIgnoreCase(status)) {
            return "下载失败";
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
