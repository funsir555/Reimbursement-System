// 这里是 UserCenterController 的后端接口入口。
// 它主要负责接收请求、校验权限并调用下游 Service。
// 如果改错，最容易影响这一组接口的查询、保存或状态流转。

package com.finex.auth.controller;

import com.finex.auth.dto.BankAccountVO;
import com.finex.auth.dto.ChangePasswordDTO;
import com.finex.auth.dto.DownloadCenterVO;
import com.finex.auth.dto.PersonalCenterVO;
import com.finex.auth.dto.UserBankAccountSaveDTO;
import com.finex.auth.dto.UserBankAccountStatusDTO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 这是 UserCenterController 控制器。
 * 它主要负责接收请求、校验权限并调用下游 Service。
 * 具体业务规则以 Service 层为准。
 */
@RestController
@RequestMapping("/auth/user-center")
@RequiredArgsConstructor
public class UserCenterController {

    private static final String PROFILE_VIEW = "profile:view";
    private static final String PROFILE_DOWNLOADS_VIEW = "profile:downloads:view";
    private static final String PROFILE_PASSWORD_UPDATE = "profile:password:update";

    private final UserCenterService userCenterService;
    private final AccessControlService accessControlService;

    // 处理 profile 请求。
    @GetMapping("/profile")
    public Result<PersonalCenterVO> profile(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, PROFILE_VIEW);
        return Result.success(userCenterService.getPersonalCenter(userId));
    }

    // 处理 listBankAccounts 请求。
    @GetMapping("/bank-accounts")
    public Result<List<BankAccountVO>> listBankAccounts(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, PROFILE_VIEW);
        return Result.success(userCenterService.listBankAccounts(userId));
    }

    // 处理 createBankAccount 请求。
    @PostMapping("/bank-accounts")
    public Result<BankAccountVO> createBankAccount(
            @Valid @RequestBody UserBankAccountSaveDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, PROFILE_VIEW);
        return Result.success("个人银行账户已新增", userCenterService.createBankAccount(userId, dto));
    }

    // 处理 updateBankAccount 请求。
    @PutMapping("/bank-accounts/{accountId}")
    public Result<BankAccountVO> updateBankAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody UserBankAccountSaveDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, PROFILE_VIEW);
        return Result.success("个人银行账户已更新", userCenterService.updateBankAccount(userId, accountId, dto));
    }

    // 处理 updateBankAccountStatus 请求。
    @PostMapping("/bank-accounts/{accountId}/status")
    public Result<Boolean> updateBankAccountStatus(
            @PathVariable Long accountId,
            @Valid @RequestBody UserBankAccountStatusDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, PROFILE_VIEW);
        String message = dto.getStatus() == 1 ? "个人银行账户已启用" : "个人银行账户已停用";
        return Result.success(message, userCenterService.updateBankAccountStatus(userId, accountId, dto.getStatus()));
    }

    // 处理 setDefaultBankAccount 请求。
    @PostMapping("/bank-accounts/{accountId}/default")
    public Result<Boolean> setDefaultBankAccount(
            @PathVariable Long accountId,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, PROFILE_VIEW);
        return Result.success("个人银行账户已设为默认", userCenterService.setDefaultBankAccount(userId, accountId));
    }

    // 处理 downloads 请求。
    @GetMapping("/downloads")
    public Result<DownloadCenterVO> downloads(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, PROFILE_VIEW, PROFILE_DOWNLOADS_VIEW);
        return Result.success(userCenterService.getDownloadCenter(userId));
    }

    // 处理 downloadContent 请求。
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

    // 处理 changePassword 请求。
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
