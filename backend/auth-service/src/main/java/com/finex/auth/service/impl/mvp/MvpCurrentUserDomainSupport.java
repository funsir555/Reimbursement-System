package com.finex.auth.service.impl.mvp;

import cn.hutool.core.util.StrUtil;
import com.finex.auth.dto.UserProfileVO;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.UserService;

public class MvpCurrentUserDomainSupport extends AbstractMvpDomainSupport {

    public MvpCurrentUserDomainSupport(
            UserService userService,
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            ExpenseDocumentService expenseDocumentService
    ) {
        super(userService, asyncTaskRecordMapper, expenseDocumentService);
    }

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
