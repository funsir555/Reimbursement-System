package com.finex.auth.service.impl.expensevoucher;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpenseVoucherTransactionRunnerTest {

    @Test
    void failedDocumentRollsBackWithoutUndoingSuccessAndCanBeRetried() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:voucher_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1",
                "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE voucher_probe (document_code VARCHAR(32) PRIMARY KEY)");
        ExpenseVoucherTransactionRunner runner = new ExpenseVoucherTransactionRunner(
                new DataSourceTransactionManager(dataSource));

        runner.execute(() -> {
            jdbc.update("INSERT INTO voucher_probe(document_code) VALUES (?)", "DOC-OK");
            return null;
        });
        assertThrows(IllegalStateException.class, () -> runner.execute(() -> {
            jdbc.update("INSERT INTO voucher_probe(document_code) VALUES (?)", "DOC-RETRY");
            throw new IllegalStateException("simulated voucher failure");
        }));
        assertThat(jdbc.queryForList("SELECT document_code FROM voucher_probe ORDER BY document_code", String.class))
                .containsExactly("DOC-OK");

        assertThrows(DataAccessException.class, () -> runner.execute(() -> {
            jdbc.update("INSERT INTO voucher_probe(document_code) VALUES (?)", "DOC-OK");
            return null;
        }));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM voucher_probe", Integer.class)).isEqualTo(1);

        runner.execute(() -> {
            jdbc.update("INSERT INTO voucher_probe(document_code) VALUES (?)", "DOC-RETRY");
            return null;
        });
        assertThat(jdbc.queryForList("SELECT document_code FROM voucher_probe ORDER BY document_code", String.class))
                .containsExactly("DOC-OK", "DOC-RETRY");
    }

    @Test
    void documentCommitIsIndependentOfOuterBatchRollback() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:voucher_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1",
                "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE voucher_probe (document_code VARCHAR(32) PRIMARY KEY)");
        DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource);
        ExpenseVoucherTransactionRunner runner = new ExpenseVoucherTransactionRunner(manager);
        TransactionTemplate outer = new TransactionTemplate(manager);

        assertThrows(IllegalStateException.class, () -> outer.execute(status -> {
            jdbc.update("INSERT INTO voucher_probe(document_code) VALUES (?)", "OUTER");
            runner.execute(() -> {
                jdbc.update("INSERT INTO voucher_probe(document_code) VALUES (?)", "INNER");
                return null;
            });
            throw new IllegalStateException("batch failure");
        }));

        List<String> committed = jdbc.queryForList(
                "SELECT document_code FROM voucher_probe ORDER BY document_code", String.class);
        assertThat(committed).containsExactly("INNER");
    }
}
