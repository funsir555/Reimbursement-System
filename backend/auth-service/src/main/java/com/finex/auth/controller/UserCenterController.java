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
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

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
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, PROFILE_VIEW);
        return Result.success(userCenterService.getPersonalCenter(userId));
    }

    @GetMapping("/downloads")
    public Result<DownloadCenterVO> downloads(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, PROFILE_VIEW, PROFILE_DOWNLOADS_VIEW);
        return Result.success(userCenterService.getDownloadCenter(userId));
    }

    @GetMapping("/downloads/{downloadId}/content")
    public ResponseEntity<Resource> downloadContent(
            @PathVariable Long downloadId,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, PROFILE_VIEW, PROFILE_DOWNLOADS_VIEW);
        UserCenterService.DownloadContent content = userCenterService.loadDownloadContent(userId, downloadId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(content.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(content.contentLength())
                .body(content.resource());
    }

    @PostMapping("/password")
    public Result<Boolean> changePassword(
            @Valid @RequestBody ChangePasswordDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, PROFILE_PASSWORD_UPDATE);
        userCenterService.changePassword(userId, dto);
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
