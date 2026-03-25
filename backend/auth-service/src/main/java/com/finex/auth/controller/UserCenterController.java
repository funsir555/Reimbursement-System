package com.finex.auth.controller;

import com.finex.auth.dto.ChangePasswordDTO;
import com.finex.auth.dto.DownloadCenterVO;
import com.finex.auth.dto.PersonalCenterVO;
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

    private final UserCenterService userCenterService;

    @GetMapping("/profile")
    public Result<PersonalCenterVO> profile(HttpServletRequest request) {
        return Result.success(userCenterService.getPersonalCenter(getCurrentUserId(request)));
    }

    @GetMapping("/downloads")
    public Result<DownloadCenterVO> downloads(HttpServletRequest request) {
        return Result.success(userCenterService.getDownloadCenter(getCurrentUserId(request)));
    }

    @PostMapping("/password")
    public Result<Boolean> changePassword(
            @Valid @RequestBody ChangePasswordDTO dto,
            HttpServletRequest request
    ) {
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
