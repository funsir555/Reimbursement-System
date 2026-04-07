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
 * 璁よ瘉鎺у埗鍣? */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    /**
     * 鐢ㄦ埛鐧诲綍
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO loginDTO) {
        try {
            LoginVO loginVO = userService.login(loginDTO);
            return Result.success("鐧诲綍鎴愬姛", loginVO);
        } catch (RuntimeException ex) {
            log.error("Login failed for username={}", loginDTO.getUsername(), ex);
            return Result.error(resolveLoginErrorMessage(ex));
        }
    }

    /**
     * 娴嬭瘯鎺ュ彛
     */
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("鏈嶅姟杩愯姝ｅ父");
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
                return "鏁版嵁搴撹繛鎺ュけ璐ワ紝璇锋鏌?FINEX_DB_USERNAME 鍜?FINEX_DB_PASSWORD 閰嶇疆";
            }
            if (lowerCaseMessage.contains("communications link failure")
                    || lowerCaseMessage.contains("connection refused")
                    || lowerCaseMessage.contains("the driver has not received any packets")) {
                return "鏁版嵁搴撴湭杩炴帴锛岃纭 MySQL 宸插惎鍔ㄤ笖 FINEX_DB_URL 閰嶇疆姝ｇ‘";
            }
            if (lowerCaseMessage.contains("unknown database")) {
                return "鏁版嵁搴?finex_db 涓嶅瓨鍦紝璇峰厛鎵ц鍒濆鍖?SQL";
            }
        }

        return "鐧诲綍澶辫触锛岃妫€鏌ユ暟鎹簱閰嶇疆鍜屽垵濮嬪寲鐘舵€?";
    }
}
