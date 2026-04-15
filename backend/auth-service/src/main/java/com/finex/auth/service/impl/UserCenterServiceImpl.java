// 业务域：个人中心与下载
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 个人中心页面、下载中心接口，下游会继续协调 个人信息、银行卡账户和下载记录。
// 风险提醒：改坏后最容易影响 个人信息展示、银行卡维护和下载留痕。

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

/**
 * UserCenterServiceImpl：service 入口实现。
 * 接住上层请求，并把 用户中心相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 个人信息展示、银行卡维护和下载留痕是否会被一起带坏。
 */
@Service
public class UserCenterServiceImpl implements UserCenterService {

    private final ProfileCenterDomainSupport profileCenterDomainSupport;
    private final ProfileBankAccountDomainSupport profileBankAccountDomainSupport;
    private final ProfileDownloadDomainSupport profileDownloadDomainSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
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

    /**
     * 获取个人中心。
     */
    @Override
    public PersonalCenterVO getPersonalCenter(Long userId) {
        return profileCenterDomainSupport.getPersonalCenter(userId);
    }

    /**
     * 查询银行账户列表。
     */
    @Override
    public List<BankAccountVO> listBankAccounts(Long userId) {
        return profileBankAccountDomainSupport.listBankAccounts(userId);
    }

    /**
     * 创建银行账户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankAccountVO createBankAccount(Long userId, UserBankAccountSaveDTO dto) {
        return profileBankAccountDomainSupport.createBankAccount(userId, dto);
    }

    /**
     * 更新银行账户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankAccountVO updateBankAccount(Long userId, Long accountId, UserBankAccountSaveDTO dto) {
        return profileBankAccountDomainSupport.updateBankAccount(userId, accountId, dto);
    }

    /**
     * 更新银行账户Status。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateBankAccountStatus(Long userId, Long accountId, Integer status) {
        return profileBankAccountDomainSupport.updateBankAccountStatus(userId, accountId, status);
    }

    /**
     * 处理用户中心中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean setDefaultBankAccount(Long userId, Long accountId) {
        return profileBankAccountDomainSupport.setDefaultBankAccount(userId, accountId);
    }

    /**
     * 获取下载中心。
     */
    @Override
    public DownloadCenterVO getDownloadCenter(Long userId) {
        return profileDownloadDomainSupport.getDownloadCenter(userId);
    }

    /**
     * 加载下载Content。
     */
    @Override
    public DownloadContent loadDownloadContent(Long userId, Long downloadId) {
        return profileDownloadDomainSupport.loadDownloadContent(userId, downloadId);
    }

    /**
     * 处理用户中心中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        profileCenterDomainSupport.changePassword(userId, dto);
    }
}
