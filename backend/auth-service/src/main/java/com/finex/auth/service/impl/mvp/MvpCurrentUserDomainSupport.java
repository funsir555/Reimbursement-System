// 业务域：首页看板与当前用户
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 MvpController 和首页页面请求，下游会继续协调 用户信息、待办汇总和发票等首页数据。
// 风险提醒：改坏后最容易影响 首页统计、个人信息与待办展示。

package com.finex.auth.service.impl.mvp;

import cn.hutool.core.util.StrUtil;
import com.finex.auth.dto.UserProfileVO;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.UserService;

/**
 * MvpCurrentUserDomainSupport：领域规则支撑类。
 * 承接 当前用户的核心业务规则。
 * 改这里时，要特别关注 首页统计、个人信息与待办展示是否会被一起带坏。
 */
public class MvpCurrentUserDomainSupport extends AbstractMvpDomainSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public MvpCurrentUserDomainSupport(
            UserService userService,
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            ExpenseDocumentService expenseDocumentService
    ) {
        super(userService, asyncTaskRecordMapper, expenseDocumentService);
    }

    /**
     * 获取当前用户。
     */
    public UserProfileVO getCurrentUser(Long userId) {
        var user = requireUser(userId);
        UserProfileVO profile = new UserProfileVO();
        profile.setUserId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setName(getDisplayName(user));
        profile.setPhone(user.getPhone());
        profile.setEmail(user.getEmail());
        profile.setPosition(StrUtil.blankToDefault(user.getPosition(), "Employee"));
        profile.setLaborRelationBelong(StrUtil.blankToDefault(user.getLaborRelationBelong(), "Headquarters"));
        profile.setCompanyId(user.getCompanyId());
        profile.setRoles(userService().getRoleCodes(userId));
        profile.setPermissionCodes(userService().getPermissionCodes(userId));
        return profile;
    }
}
