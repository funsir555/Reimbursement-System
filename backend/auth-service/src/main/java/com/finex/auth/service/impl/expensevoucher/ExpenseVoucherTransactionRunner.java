package com.finex.auth.service.impl.expensevoucher;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * 凭证批量推送的单据级事务执行器。
 * 每一张单据使用独立的新事务，允许批次在业务上部分成功。
 */
@Component
public class ExpenseVoucherTransactionRunner {

    private final TransactionTemplate requiresNew;

    public ExpenseVoucherTransactionRunner(PlatformTransactionManager transactionManager) {
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    public <T> T execute(Supplier<T> action) {
        return requiresNew.execute(status -> action.get());
    }
}
