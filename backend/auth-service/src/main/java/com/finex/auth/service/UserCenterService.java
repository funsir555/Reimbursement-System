// 业务域：个人中心与下载
// 文件角色：service 接口
// 上下游关系：上游通常来自 个人中心页面、下载中心接口，下游会继续协调 个人信息、银行卡账户和下载记录。
// 风险提醒：改坏后最容易影响 个人信息展示、银行卡维护和下载留痕。

package com.finex.auth.service;

import com.finex.auth.dto.BankAccountVO;
import com.finex.auth.dto.ChangePasswordDTO;
import com.finex.auth.dto.DownloadCenterVO;
import com.finex.auth.dto.PersonalCenterVO;
import com.finex.auth.dto.UserBankAccountSaveDTO;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * UserCenterService：service 接口。
 * 定义用户中心这块对外提供的业务入口能力。
 * 改这里时，要特别关注 个人信息展示、银行卡维护和下载留痕是否会被一起带坏。
 */
public interface UserCenterService {

    /**
     * 获取个人中心。
     */
    PersonalCenterVO getPersonalCenter(Long userId);

    /**
     * 查询银行账户列表。
     */
    List<BankAccountVO> listBankAccounts(Long userId);

    /**
     * 创建银行账户。
     */
    BankAccountVO createBankAccount(Long userId, UserBankAccountSaveDTO dto);

    /**
     * 更新银行账户。
     */
    BankAccountVO updateBankAccount(Long userId, Long accountId, UserBankAccountSaveDTO dto);

    /**
     * 更新银行账户Status。
     */
    Boolean updateBankAccountStatus(Long userId, Long accountId, Integer status);

    Boolean setDefaultBankAccount(Long userId, Long accountId);

    /**
     * 获取下载中心。
     */
    DownloadCenterVO getDownloadCenter(Long userId);

    /**
     * 加载下载Content。
     */
    DownloadContent loadDownloadContent(Long userId, Long downloadId);

    void changePassword(Long userId, ChangePasswordDTO dto);

    /**
     * 下载Content。
     */
    record DownloadContent(Resource resource, String fileName, long contentLength) {
    }
}
