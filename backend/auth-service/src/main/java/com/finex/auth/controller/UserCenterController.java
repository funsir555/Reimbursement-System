package com.finex.auth.controller;

import com.finex.auth.dto.ChangePasswordDTO;
import com.finex.auth.dto.DownloadCenterVO;
import com.finex.auth.dto.PersonalCenterVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.UserCenterService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/user-center")
@RequiredArgsConstructor
public class UserCenterController {

    private static final String PROFILE_VIEW = "profile:view";
    private static final String PROFILE_DOWNLOADS_VIEW = "profile:downloads:view";
    private static final String PROFILE_PASSWORD_UPDATE = "profile:password:update";

    private final UserCenterService userCenterService;
    private final AccessControlService accessControlService;

    @GetMapping("/profile")
    public Result<PersonalCenterVO> profile(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROFILE_VIEW);
        return Result.success(userCenterService.getPersonalCenter(getCurrentUserId(request)));
    }

    @GetMapping("/downloads")
    public Result<DownloadCenterVO> downloads(HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), PROFILE_VIEW, PROFILE_DOWNLOADS_VIEW);
        return Result.success(userCenterService.getDownloadCenter(getCurrentUserId(request)));
    }

    @PostMapping("/password")
    public Result<Boolean> changePassword(
            @Valid @RequestBody ChangePasswordDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROFILE_PASSWORD_UPDATE);
        userCenterService.changePassword(getCurrentUserId(request), dto);
        return Result.success("密码修改成功", Boolean.TRUE);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId instanceof Long value) {
            return value;
        }
        if (userId instanceof Integer value) {
            return value.longValue();
        }
        throw new IllegalStateException("无法获取当前登录用户");
    }
}
