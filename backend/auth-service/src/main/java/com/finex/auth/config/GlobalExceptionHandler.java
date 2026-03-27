package com.finex.auth.config;

import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return Result.badRequest(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("请求参数校验失败");
        return Result.badRequest(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException ex) {
        return Result.badRequest(ex.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public Result<Void> handleSecurityException(SecurityException ex) {
        return Result.forbidden(ex.getMessage());
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    public Result<Void> handleBadSqlGrammar(BadSqlGrammarException ex, HttpServletRequest request) {
        String message = resolveDatabaseMessage(ex);
        log.error("Database exception on {}: {}", request.getRequestURI(), message, ex);
        return Result.error(message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return Result.error("系统异常，请稍后重试");
    }

    private String resolveDatabaseMessage(BadSqlGrammarException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        if (message != null && message.contains("pm_custom_archive")) {
            return "自定义档案相关表未初始化，请先执行 backend/sql/init_custom_archive.sql";
        }
        return "数据库表或字段未初始化，请先执行最新 SQL 脚本";
    }
}
