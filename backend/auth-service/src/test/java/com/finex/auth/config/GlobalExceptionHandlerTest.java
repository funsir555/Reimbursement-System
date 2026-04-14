package com.finex.auth.config;

import com.finex.common.Result;
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLDataException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    @Test
    void resolveDatabaseMessagePointsExpenseCreateInitializationToIncrementalScript() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BadSqlGrammarException exception = new BadSqlGrammarException(
                "load templates",
                "SELECT * FROM pm_document_instance",
                new SQLSyntaxErrorException("Table 'finex_db.pm_document_instance' doesn't exist")
        );

        String message = (String) ReflectionTestUtils.invokeMethod(handler, "resolveDatabaseMessage", exception);

        assertTrue(message.contains("backend/sql/init_expense_create_incremental.sql"));
    }

    @Test
    void handleDatabaseWrappedPointsLegacyActionLogColumnToIncrementalScript() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        PersistenceException exception = new PersistenceException(
                "insert log failed",
                new SQLSyntaxErrorException("Unknown column 'payload_json' in 'field list'")
        );

        Result<Void> result = handler.handleDatabaseWrapped(
                exception,
                new MockHttpServletRequest("POST", "/auth/expenses/create/documents")
        );

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("backend/sql/init_expense_create_incremental.sql"));
    }

    @Test
    void handleDatabaseWrappedPointsLegacyExpenseDetailUniqueIndexToMigrationScript() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        PersistenceException exception = new PersistenceException(
                "insert expense detail failed",
                new SQLIntegrityConstraintViolationException("Duplicate entry 'D001' for key 'uk_pm_document_expense_detail_no'")
        );

        Result<Void> result = handler.handleDatabaseWrapped(
                exception,
                new MockHttpServletRequest("POST", "/auth/expenses/create/documents")
        );

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("backend/sql/migrate_expense_detail_detail_no_unique_index.sql"));
        assertTrue(!result.getMessage().contains("init_expense_create_incremental.sql"));
    }

    @Test
    void handleIllegalStateReturnsBusinessMessageForTemplateBindingErrors() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        IllegalStateException exception = new IllegalStateException("\u5f53\u524d\u5ba1\u6279\u6a21\u677f\u7ed1\u5b9a\u7684\u6d41\u7a0b\u4e0d\u5b58\u5728\uff0c\u8bf7\u5148\u4fee\u590d\u6a21\u677f\u914d\u7f6e");

        Result<Void> result = handler.handleIllegalState(
                exception,
                new MockHttpServletRequest("POST", "/auth/expenses/create/documents")
        );

        assertEquals(500, result.getCode());
        assertEquals("\u5f53\u524d\u5ba1\u6279\u6a21\u677f\u7ed1\u5b9a\u7684\u6d41\u7a0b\u4e0d\u5b58\u5728\uff0c\u8bf7\u5148\u4fee\u590d\u6a21\u677f\u914d\u7f6e", result.getMessage());
    }

    @Test
    void handleIllegalStateFallsBackToGenericMessageForInternalErrors() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        IllegalStateException exception = new IllegalStateException("Failed to parse flow snapshot");

        Result<Void> result = handler.handleIllegalState(
                exception,
                new MockHttpServletRequest("POST", "/auth/expenses/create/documents")
        );

        assertEquals(500, result.getCode());
        assertEquals("\u7cfb\u7edf\u5f02\u5e38\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5", result.getMessage());
    }

    @Test
    void handleIllegalStateReturnsFinanceSystemChineseMessageDirectly() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        IllegalStateException exception = new IllegalStateException("账套模板已停用");

        Result<Void> result = handler.handleIllegalState(
                exception,
                new MockHttpServletRequest("POST", "/auth/finance/system-management/account-sets/create")
        );

        assertEquals(500, result.getCode());
        assertEquals("账套模板已停用", result.getMessage());
    }

    @Test
    void handleIllegalStateDoesNotBroadenFinanceSystemPassthroughToOtherPaths() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        IllegalStateException exception = new IllegalStateException("账套模板已停用");

        Result<Void> result = handler.handleIllegalState(
                exception,
                new MockHttpServletRequest("POST", "/auth/expenses/create/documents")
        );

        assertEquals(500, result.getCode());
        assertEquals("\u7cfb\u7edf\u5f02\u5e38\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5", result.getMessage());
    }

    @Test
    void resolveDatabaseMessageReturnsFriendlyTextForDataTooLong() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        PersistenceException exception = new PersistenceException(
                "save fixed asset failed",
                new SQLDataException("Data truncation: Data too long for column 'asset_code' at row 1")
        );

        String message = (String) ReflectionTestUtils.invokeMethod(handler, "resolveDatabaseMessage", exception);

        assertEquals("\u63d0\u4ea4\u5185\u5bb9\u8d85\u8fc7\u5b57\u6bb5\u957f\u5ea6\u9650\u5236\uff0c\u8bf7\u68c0\u67e5\u7f16\u7801\u3001\u540d\u79f0\u7b49\u8f93\u5165\u957f\u5ea6\u540e\u91cd\u8bd5", message);
    }
}
