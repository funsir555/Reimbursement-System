// 这里是 AuthController 的后端接口入口。
// 它主要负责接收请求、校验权限并调用下游 Service。
// 如果改错，最容易影响这一组接口的查询、保存或状态流转。

package com.finex.auth.controller;

import com.finex.auth.dto.LoginDTO;
import com.finex.auth.dto.LoginVO;
import com.finex.auth.service.UserService;
import com.finex.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 这是 AuthController 控制器。
 * 它主要负责接收请求、校验权限并调用下游 Service。
 * 具体业务规则以 Service 层为准。
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    /**
     * 登录接口
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO loginDTO) {
        try {
            LoginVO loginVO = userService.login(loginDTO);
            return Result.success("登录成功", loginVO);
        } catch (RuntimeException ex) {
            log.error("Login failed for username={}", loginDTO.getUsername(), ex);
            return Result.error(resolveLoginErrorMessage(ex));
        }
    }

    /**
     * 测试接口
     */
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("接口访问成功");
    }

    private String resolveLoginErrorMessage(RuntimeException ex) {
        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            return ex.getMessage();
        }

        Throwable rootCause = ex;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        String rootMessage = rootCause.getMessage();
        if (rootMessage != null) {
            String lowerCaseMessage = rootMessage.toLowerCase();
            if (lowerCaseMessage.contains("access denied")) {
                return "数据库账号或密码错误，请检查 FINEX_DB_USERNAME 和 FINEX_DB_PASSWORD 配置。";
            }
            if (lowerCaseMessage.contains("communications link failure")
                    || lowerCaseMessage.contains("connection refused")
                    || lowerCaseMessage.contains("the driver has not received any packets")) {
                return "数据库连接失败，请确认 MySQL 已启动且 FINEX_DB_URL 配置正确。";
            }
            if (lowerCaseMessage.contains("unknown database")) {
                return "数据库 finex_db 不存在，请先执行初始化 SQL。";
            }
        }

        return "登录失败，系统当前无法完成身份验证，请检查服务和数据库配置后重试。";
    }
}
