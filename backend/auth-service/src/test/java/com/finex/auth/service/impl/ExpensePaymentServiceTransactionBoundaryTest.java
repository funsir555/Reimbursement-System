package com.finex.auth.service.impl;

import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/** 验证真实服务入口的 Spring 事务代理，不连接银行或业务数据库。 */
class ExpensePaymentServiceTransactionBoundaryTest {

    @Test
    void failedPaymentStartRollsBackLocalWritesAndRetryCanCommit() {
        DriverManagerDataSource dataSource = isolatedDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE payment_tx_probe (document_code VARCHAR(64), item VARCHAR(32), PRIMARY KEY (document_code, item))");
        ExpensePaymentWorkflowService workflow = mock(ExpensePaymentWorkflowService.class);
        ExpenseDocumentServiceImpl service = transactionalService(dataSource, workflow);

        doAnswer(invocation -> {
            jdbc.update("INSERT INTO payment_tx_probe(document_code, item) VALUES (?, ?)", "DOC-TEST", "record");
            jdbc.update("INSERT INTO payment_tx_probe(document_code, item) VALUES (?, ?)", "DOC-TEST", "runtime");
            throw new IllegalStateException("simulated payment failure after local writes");
        }).when(workflow).startPaymentTask(9L, "tester", 1L);

        assertThrows(IllegalStateException.class, () -> service.startPaymentTask(9L, "tester", 1L));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM payment_tx_probe", Integer.class));

        doAnswer(invocation -> {
            jdbc.update("INSERT INTO payment_tx_probe(document_code, item) VALUES (?, ?)", "DOC-TEST", "record");
            jdbc.update("INSERT INTO payment_tx_probe(document_code, item) VALUES (?, ?)", "DOC-TEST", "runtime");
            return new ExpenseDocumentDetailVO();
        }).when(workflow).startPaymentTask(9L, "tester", 1L);

        service.startPaymentTask(9L, "tester", 1L);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM payment_tx_probe", Integer.class));
    }

    @Test
    void callbackFailureAlsoRollsBackLocalFailureMarker() {
        // Characterizes the current transaction boundary, not the desired bank-failure persistence contract.
        DriverManagerDataSource dataSource = isolatedDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE callback_tx_probe (document_code VARCHAR(64) PRIMARY KEY, status VARCHAR(32))");
        jdbc.update("INSERT INTO callback_tx_probe(document_code, status) VALUES (?, ?)", "DOC-TEST", "PENDING");
        ExpensePaymentWorkflowService workflow = mock(ExpensePaymentWorkflowService.class);
        ExpenseDocumentServiceImpl service = transactionalService(dataSource, workflow);
        ExpenseBankCallbackDTO callback = new ExpenseBankCallbackDTO();
        callback.setDocumentCode("DOC-TEST");

        doAnswer(invocation -> {
            jdbc.update("UPDATE callback_tx_probe SET status = ? WHERE document_code = ?", "FAILED", "DOC-TEST");
            throw new IllegalStateException("simulated bank failure callback");
        }).when(workflow).handleCmbCloudCallback(callback);

        assertThrows(IllegalStateException.class, () -> service.handleCmbCloudCallback(callback));
        assertEquals("PENDING", jdbc.queryForObject(
                "SELECT status FROM callback_tx_probe WHERE document_code = ?", String.class, "DOC-TEST"));
    }

    private static DriverManagerDataSource isolatedDataSource() {
        return new DriverManagerDataSource(
                "jdbc:h2:mem:payment_tx_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1",
                "sa", "");
    }

    private static ExpenseDocumentServiceImpl transactionalService(
            DriverManagerDataSource dataSource,
            ExpensePaymentWorkflowService workflow
    ) {
        ExpenseDocumentServiceImpl target = new ExpenseDocumentServiceImpl(
                null, null, null, workflow, null, null);
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(new TransactionInterceptor(
                new DataSourceTransactionManager(dataSource), new AnnotationTransactionAttributeSource()));
        return (ExpenseDocumentServiceImpl) proxyFactory.getProxy();
    }
}
