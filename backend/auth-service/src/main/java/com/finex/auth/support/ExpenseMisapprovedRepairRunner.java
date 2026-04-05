package com.finex.auth.support;

import com.finex.auth.service.impl.ExpenseDocumentServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "finex.expense", name = "repair-misapproved-on-startup", havingValue = "true")
public class ExpenseMisapprovedRepairRunner implements ApplicationRunner {

    private final ExpenseDocumentServiceImpl expenseDocumentService;
    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        List<String> repairedDocumentCodes = expenseDocumentService.repairMisapprovedDocumentsByRootContainerBug();
        log.info("Expense misapproved repair completed repairedCount={} documents={}", repairedDocumentCodes.size(), repairedDocumentCodes);
        int exitCode = SpringApplication.exit(applicationContext, () -> 0);
        System.exit(exitCode);
    }
}
