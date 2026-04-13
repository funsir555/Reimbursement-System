package com.finex.auth.config;

import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.PersistenceException;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String GENERIC_SYSTEM_ERROR = "\u7cfb\u7edf\u5f02\u5e38\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5";
    private static final String DATA_TOO_LONG_MESSAGE = "\u63d0\u4ea4\u5185\u5bb9\u8d85\u8fc7\u5b57\u6bb5\u957f\u5ea6\u9650\u5236\uff0c\u8bf7\u68c0\u67e5\u7f16\u7801\u3001\u540d\u79f0\u7b49\u8f93\u5165\u957f\u5ea6\u540e\u91cd\u8bd5";
    private static final String CUSTOM_ARCHIVE_INIT_MESSAGE = "\u81ea\u5b9a\u4e49\u6863\u6848\u76f8\u5173\u8868\u672a\u521d\u59cb\u5316\uff0c\u8bf7\u5148\u6267\u884c backend/sql/init_custom_archive.sql";
    private static final String EXPENSE_CREATE_INIT_MESSAGE = "\u5ba1\u6279\u5355\u521b\u5efa\u76f8\u5173\u8868\u6216\u5b57\u6bb5\u672a\u521d\u59cb\u5316\uff0c\u8bf7\u5148\u6267\u884c backend/sql/init_expense_create_incremental.sql";
    private static final String LEGACY_EXPENSE_DETAIL_INDEX_MESSAGE = "\u8d39\u7528\u660e\u7ec6\u7f16\u53f7\u7d22\u5f15\u4ecd\u662f\u65e7\u7ed3\u6784\uff0c\u8bf7\u5148\u6267\u884c backend/sql/migrate_expense_detail_detail_no_unique_index.sql \u540e\u91cd\u8bd5";

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return Result.badRequest(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        String message = resolveBusinessStateMessage(ex);
        if (message == null) {
            return handleException(ex, request);
        }
        log.warn("Business exception on {}: {}", request.getRequestURI(), message, ex);
        return Result.error(message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("\u8bf7\u6c42\u53c2\u6570\u6821\u9a8c\u5931\u8d25");
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
        return handleDatabaseException(ex, request);
    }

    @ExceptionHandler({DataAccessException.class, PersistenceException.class, MyBatisSystemException.class})
    public Result<Void> handleDatabaseWrapped(Exception ex, HttpServletRequest request) {
        return handleDatabaseException(ex, request);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex, HttpServletRequest request) {
        Throwable rootCause = getRootCause(ex);
        log.error(
                "Unhandled exception on {}: rootType={} rootMessage={}",
                request.getRequestURI(),
                rootCause.getClass().getSimpleName(),
                resolveThrowableMessage(ex),
                ex
        );
        return Result.error(GENERIC_SYSTEM_ERROR);
    }

    private Result<Void> handleDatabaseException(Exception ex, HttpServletRequest request) {
        String message = resolveDatabaseMessage(ex);
        Throwable rootCause = getRootCause(ex);
        log.error(
                "Database exception on {}: rootType={} resolvedMessage={}",
                request.getRequestURI(),
                rootCause.getClass().getSimpleName(),
                message,
                ex
        );
        return Result.error(message);
    }

    private String resolveDatabaseMessage(Throwable ex) {
        String message = resolveThrowableMessage(ex);
        if (isDataTooLongMessage(message)) {
            return DATA_TOO_LONG_MESSAGE;
        }
        if (isLegacyExpenseDetailNoIndexConflict(message)) {
            return LEGACY_EXPENSE_DETAIL_INDEX_MESSAGE;
        }
        if (isMissingSqlObjectMessage(message) && message != null && message.contains("pm_custom_archive")) {
            return CUSTOM_ARCHIVE_INIT_MESSAGE;
        }
        if (isMissingSqlObjectMessage(message) && isExpenseCreateInitializationMissing(message)) {
            return EXPENSE_CREATE_INIT_MESSAGE;
        }
        return GENERIC_SYSTEM_ERROR;
    }

    private String resolveBusinessStateMessage(IllegalStateException ex) {
        String message = trimToNull(ex.getMessage());
        if (message == null || message.startsWith("Failed to ")) {
            return null;
        }
        if (message.contains("Available template not found")) {
            return "\u5f53\u524d\u5ba1\u6279\u6a21\u677f\u4e0d\u5b58\u5728\u6216\u5df2\u505c\u7528";
        }
        if (message.contains("Document not found")) {
            return "\u5f53\u524d\u5355\u636e\u4e0d\u5b58\u5728";
        }
        if (message.contains("Approval task not found")) {
            return "\u5f53\u524d\u5ba1\u6279\u4efb\u52a1\u4e0d\u5b58\u5728";
        }
        if (message.contains("Task has already been handled")) {
            return "\u5f53\u524d\u5ba1\u6279\u4efb\u52a1\u5df2\u5904\u7406";
        }
        if (message.contains("Current user cannot view") || message.contains("Current user cannot handle")) {
            return "\u5f53\u524d\u7528\u6237\u6ca1\u6709\u6743\u9650\u6267\u884c\u8be5\u64cd\u4f5c";
        }
        if (message.startsWith("\u5f53\u524d") || message.startsWith("\u53ea\u6709") || message.startsWith("\u540c\u4e00")) {
            return message;
        }
        return null;
    }

    private boolean isExpenseCreateInitializationMissing(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        return message.contains("pm_code_sequence")
                || message.contains("pm_expense_detail_design")
                || message.contains("pm_document_expense_detail")
                || message.contains("pm_document_instance")
                || message.contains("pm_document_task")
                || message.contains("pm_document_action_log")
                || message.contains("gl_Vender")
                || message.contains("expense_detail_design_code")
                || message.contains("expense_detail_mode_default")
                || message.contains("current_node_key")
                || message.contains("current_node_name")
                || message.contains("current_task_type")
                || message.contains("finished_at")
                || message.contains("task_kind")
                || message.contains("source_task_id")
                || message.contains("node_type")
                || message.contains("task_batch_no")
                || message.contains("assignee_user_id")
                || message.contains("assignee_name")
                || message.contains("action_comment")
                || message.contains("payload_json")
                || message.contains("template_snapshot_json")
                || message.contains("form_schema_snapshot_json")
                || message.contains("flow_snapshot_json")
                || message.contains("expense_type_code")
                || message.contains("business_scene_mode")
                || message.contains("invoice_amount")
                || message.contains("actual_payment_amount")
                || message.contains("pending_write_off_amount");
    }

    private boolean isMissingSqlObjectMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("doesn't exist")
                || normalized.contains("unknown column")
                || normalized.contains("unknown table")
                || normalized.contains("unknown database")
                || normalized.contains("unknown index")
                || normalized.contains("can't open");
    }

    private boolean isLegacyExpenseDetailNoIndexConflict(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase();
        return (normalized.contains("duplicate entry") || normalized.contains("duplicate key"))
                && normalized.contains("uk_pm_document_expense_detail_no");
    }

    private boolean isDataTooLongMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("data too long for column")
                || normalized.contains("data truncation")
                || normalized.contains("string or binary data would be truncated")
                || normalized.contains("value too long for type");
    }

    private Throwable getRootCause(Throwable ex) {
        Throwable current = ex == null ? null : ex;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current == null ? ex : current;
    }

    private String resolveThrowableMessage(Throwable ex) {
        Throwable rootCause = getRootCause(ex);
        String message = trimToNull(rootCause == null ? null : rootCause.getMessage());
        if (message != null) {
            return message;
        }
        return trimToNull(ex == null ? null : ex.getMessage());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
