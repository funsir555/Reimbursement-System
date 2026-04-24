package com.finex.auth.config;

import com.finex.common.Result;
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLDataException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLSyntaxErrorException;

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
    void handleDatabaseWrappedReturnsReadableMessageForDuplicateRelationConstraint() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        PersistenceException exception = new PersistenceException(
                "insert relation failed",
                new SQLIntegrityConstraintViolationException("Duplicate entry 'DOC-001-relatedDocs-DOC-002' for key 'uk_pm_document_relation_source_target'")
        );

        Result<Void> result = handler.handleDatabaseWrapped(
                exception,
                new MockHttpServletRequest("PUT", "/auth/expenses/DOC-001/resubmit")
        );

        assertEquals(500, result.getCode());
        assertEquals("\u5173\u8054\u5355\u636e\u8bb0\u5f55\u5df2\u5b58\u5728\uff0c\u8bf7\u5237\u65b0\u9875\u9762\u540e\u91cd\u8bd5", result.getMessage());
    }

    @Test
    void handleDatabaseWrappedReturnsReadableMessageForDuplicateWriteOffConstraint() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        PersistenceException exception = new PersistenceException(
                "insert writeoff failed",
                new SQLIntegrityConstraintViolationException("Duplicate entry 'DOC-001-writeoffDocs-DOC-003' for key 'uk_pm_document_write_off_source_target'")
        );

        Result<Void> result = handler.handleDatabaseWrapped(
                exception,
                new MockHttpServletRequest("PUT", "/auth/expenses/DOC-001/resubmit")
        );

        assertEquals(500, result.getCode());
        assertEquals("\u6838\u9500\u5355\u636e\u8bb0\u5f55\u5df2\u5b58\u5728\uff0c\u8bf7\u5237\u65b0\u9875\u9762\u540e\u91cd\u8bd5", result.getMessage());
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
        IllegalStateException exception = new IllegalStateException("\u8d26\u5957\u6a21\u677f\u5df2\u505c\u7528");

        Result<Void> result = handler.handleIllegalState(
                exception,
                new MockHttpServletRequest("POST", "/auth/finance/system-management/account-sets/create")
        );

        assertEquals(500, result.getCode());
        assertEquals("\u8d26\u5957\u6a21\u677f\u5df2\u505c\u7528", result.getMessage());
    }

    @Test
    void handleIllegalStateStillKeepsUnknownEnglishMessageGenericOnExpensePaths() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        IllegalStateException exception = new IllegalStateException("Account set template disabled");

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

    @Test
    void handleIllegalStateReturnsChineseMessageDirectlyForGlRequests() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        IllegalStateException exception = new IllegalStateException("\u5ba2\u6237\u7f16\u7801\u5df2\u5b58\u5728");

        Result<Void> result = handler.handleIllegalState(
                exception,
                new MockHttpServletRequest("POST", "/auth/finance/archives/customers")
        );

        assertEquals(500, result.getCode());
        assertEquals("\u5ba2\u6237\u7f16\u7801\u5df2\u5b58\u5728", result.getMessage());
    }

    @Test
    void handleIllegalStateReturnsChineseMessageDirectlyForExpenseResubmitPath() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        IllegalStateException exception = new IllegalStateException("\u8282\u70b9\u3010\u9886\u5bfc\u5ba1\u6279\u3011\u627e\u4e0d\u5230\u5ba1\u6279\u4eba\uff0c\u5f53\u524d\u914d\u7f6e\u4e0d\u5141\u8bb8\u63d0\u4ea4");

        Result<Void> result = handler.handleIllegalState(
                exception,
                new MockHttpServletRequest("PUT", "/auth/expenses/DOC-001/resubmit")
        );

        assertEquals(500, result.getCode());
        assertEquals("\u8282\u70b9\u3010\u9886\u5bfc\u5ba1\u6279\u3011\u627e\u4e0d\u5230\u5ba1\u6279\u4eba\uff0c\u5f53\u524d\u914d\u7f6e\u4e0d\u5141\u8bb8\u63d0\u4ea4", result.getMessage());
    }

    @Test
    void handleIllegalStateReturnsChineseMessageDirectlyForExpenseCreatePath() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        IllegalStateException exception = new IllegalStateException("\u8bf7\u5148\u586b\u5199\u3010\u6536\u6b3e\u5355\u4f4d\u3011");

        Result<Void> result = handler.handleIllegalState(
                exception,
                new MockHttpServletRequest("POST", "/auth/expenses/create/documents")
        );

        assertEquals(500, result.getCode());
        assertEquals("\u8bf7\u5148\u586b\u5199\u3010\u6536\u6b3e\u5355\u4f4d\u3011", result.getMessage());
    }

    @Test
    void handleIllegalStateReturnsChineseMessageDirectlyForProcessManagementRequests() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        IllegalStateException exception = new IllegalStateException("\u6a21\u677f\u540d\u79f0\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26");

        Result<Void> result = handler.handleIllegalState(
                exception,
                new MockHttpServletRequest("POST", "/auth/process-management/templates")
        );

        assertEquals(500, result.getCode());
        assertEquals("\u6a21\u677f\u540d\u79f0\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26", result.getMessage());
    }
}
