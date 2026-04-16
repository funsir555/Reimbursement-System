// 业务域：个人中心与下载
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 个人中心页面、下载中心接口，下游会继续协调 个人信息、银行卡账户和下载记录。
// 风险提醒：改坏后最容易影响 个人信息展示、银行卡维护和下载留痕。

package com.finex.auth.service.impl.profile;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.BankAccountVO;
import com.finex.auth.dto.UserBankAccountSaveDTO;
import com.finex.auth.entity.DownloadRecord;
import com.finex.auth.entity.UserBankAccount;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.service.UserService;
import com.finex.auth.service.impl.DownloadStorageService;

import java.util.List;
import java.util.Objects;

/**
 * ProfileBankAccountDomainSupport：领域规则支撑类。
 * 承接 个人中心银行账户的核心业务规则。
 * 改这里时，要特别关注 个人信息展示、银行卡维护和下载留痕是否会被一起带坏。
 */
public final class ProfileBankAccountDomainSupport extends AbstractProfileDomainSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public ProfileBankAccountDomainSupport(
            UserService userService,
            UserBankAccountMapper userBankAccountMapper,
            DownloadRecordMapper downloadRecordMapper,
            DownloadStorageService downloadStorageService
    ) {
        super(userService, userBankAccountMapper, downloadRecordMapper, downloadStorageService);
    }

    /**
     * 查询银行账户列表。
     */
    public List<BankAccountVO> listBankAccounts(Long userId) {
        requireUser(userId);
        return loadBankAccounts(userId);
    }

    /**
     * 创建银行账户。
     */
    public BankAccountVO createBankAccount(Long userId, UserBankAccountSaveDTO dto) {
        requireUser(userId);
        UserBankAccount account = new UserBankAccount();
        applyBankAccount(account, dto);
        account.setUserId(userId);
        userBankAccountMapper().insert(account);
        if (Integer.valueOf(1).equals(account.getDefaultAccount())) {
            clearOtherDefaultBankAccounts(userId, account.getId());
        }
        return findBankAccount(userId, account.getId());
    }

    /**
     * 更新银行账户。
     */
    public BankAccountVO updateBankAccount(Long userId, Long accountId, UserBankAccountSaveDTO dto) {
        requireUser(userId);
        UserBankAccount account = requireBankAccount(userId, accountId);
        applyBankAccount(account, dto);
        userBankAccountMapper().updateById(account);
        if (Integer.valueOf(1).equals(account.getDefaultAccount())) {
            clearOtherDefaultBankAccounts(userId, accountId);
        }
        return findBankAccount(userId, accountId);
    }

    /**
     * 更新银行账户Status。
     */
    public Boolean updateBankAccountStatus(Long userId, Long accountId, Integer status) {
        requireUser(userId);
        UserBankAccount account = requireBankAccount(userId, accountId);
        account.setStatus(normalizeStatus(status));
        if (!Integer.valueOf(1).equals(account.getStatus())) {
            account.setDefaultAccount(0);
        }
        userBankAccountMapper().updateById(account);
        return Boolean.TRUE;
    }

    /**
     * 处理个人中心银行账户中的这一步。
     */
    public Boolean setDefaultBankAccount(Long userId, Long accountId) {
        requireUser(userId);
        UserBankAccount account = requireBankAccount(userId, accountId);
        if (!Integer.valueOf(1).equals(account.getStatus())) {
            throw new IllegalArgumentException("停用账户不能设为默认");
        }
        account.setDefaultAccount(1);
        userBankAccountMapper().updateById(account);
        clearOtherDefaultBankAccounts(userId, accountId);
        return Boolean.TRUE;
    }

    /**
     * 加载银行账户。
     */
    List<BankAccountVO> loadBankAccounts(Long userId) {
        return userBankAccountMapper().selectList(
                Wrappers.<UserBankAccount>lambdaQuery()
                        .eq(UserBankAccount::getUserId, userId)
                        .orderByDesc(UserBankAccount::getDefaultAccount)
                        .orderByAsc(UserBankAccount::getId)
        ).stream().map(this::toBankAccount).toList();
    }

    private UserBankAccount requireBankAccount(Long userId, Long accountId) {
        UserBankAccount account = userBankAccountMapper().selectOne(
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

    /**
     * 查询银行账户。
     */
    private BankAccountVO findBankAccount(Long userId, Long accountId) {
        return toBankAccount(requireBankAccount(userId, accountId));
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
        vo.setAccountType(defaultText(trimToNull(account.getAccountType()), "对私账户"));
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
        account.setBankCode(requireText(dto.getBankCode(), "开户银行编码不能为空"));
        account.setBankName(requireText(dto.getBankName(), "开户银行不能为空"));
        account.setProvince(requireText(dto.getProvince(), "开户省不能为空"));
        account.setCity(requireText(dto.getCity(), "开户市不能为空"));
        account.setBranchCode(requireText(dto.getBranchCode(), "分支行编码不能为空"));
        account.setBranchName(requireText(dto.getBranchName(), "分支行不能为空"));
        account.setCnapsCode(trimToNull(dto.getCnapsCode()));
        account.setStatus(normalizeStatus(dto.getStatus()));
        account.setDefaultAccount(Integer.valueOf(1).equals(account.getStatus()) && normalizeFlag(dto.getDefaultAccount()) == 1 ? 1 : 0);
    }

    /**
     * 清理Other默认银行账户。
     */
    private void clearOtherDefaultBankAccounts(Long userId, Long currentId) {
        List<UserBankAccount> accounts = userBankAccountMapper().selectList(
                Wrappers.<UserBankAccount>lambdaQuery()
                        .eq(UserBankAccount::getUserId, userId)
                        .eq(UserBankAccount::getDefaultAccount, 1)
        );
        for (UserBankAccount account : accounts) {
            if (Objects.equals(account.getId(), currentId)) {
                continue;
            }
            account.setDefaultAccount(0);
            userBankAccountMapper().updateById(account);
        }
    }

    private Integer normalizeStatus(Integer status) {
        return Integer.valueOf(0).equals(status) ? 0 : 1;
    }

    private int normalizeFlag(Integer value) {
        return Integer.valueOf(1).equals(value) ? 1 : 0;
    }

    private String maskAccountNo(String accountNo) {
        if (accountNo == null || accountNo.length() < 8) {
            return blankToDefault(accountNo, "-");
        }
        return accountNo.substring(0, 4) + " **** **** " + accountNo.substring(accountNo.length() - 4);
    }
}
