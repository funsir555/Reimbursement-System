package com.finex.auth.agent.workflow;

import com.finex.auth.agent.context.TenantContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlledQueryBranchRunnerTest {
    @Test
    void branchTimeoutCancelsTheStage() {
        ControlledQueryBranchRunner runner = new ControlledQueryBranchRunner(
                TimeUnit.MILLISECONDS.toNanos(80), TimeUnit.MILLISECONDS.toNanos(500));
        try {
            ControlledQueryBranchRunner.BranchFailure failure = assertThrows(
                    ControlledQueryBranchRunner.BranchFailure.class,
                    () -> runner.run(List.of("slow", "fast"), key -> {
                        if ("slow".equals(key)) Thread.sleep(300);
                        return ControlledToolResult.builder().success(true).build();
                    }));
            assertTrue(failure.getMessage().contains("超时"));
            assertEquals("CANCELLED", failure.logs().get("slow").status());
        } finally {
            runner.shutdown();
        }
    }

    @Test
    void stageDeadlineAlsoCancelsPendingBranches() {
        ControlledQueryBranchRunner runner = new ControlledQueryBranchRunner(
                TimeUnit.MILLISECONDS.toNanos(500), TimeUnit.MILLISECONDS.toNanos(80));
        try {
            ControlledQueryBranchRunner.BranchFailure failure = assertThrows(
                    ControlledQueryBranchRunner.BranchFailure.class,
                    () -> runner.run(List.of("one", "two"), key -> {
                        Thread.sleep(300);
                        return ControlledToolResult.builder().success(true).build();
                    }));
            assertTrue(failure.getMessage().contains("阶段超时"));
        } finally {
            runner.shutdown();
        }
    }

    @Test
    void runsConcurrentlyAndReturnsCanvasOrderWithRestoredContext() throws Exception {
        ControlledQueryBranchRunner runner = new ControlledQueryBranchRunner();
        CountDownLatch entered = new CountDownLatch(3);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger seenContext = new AtomicInteger();
        TenantContext.setTenantId("tenant-one");
        TenantContext.setCompanyId("COMPANY-A");
        TenantContext.setUserId(7L);
        try {
            Map<String, ControlledQueryBranchRunner.BranchResult> results = runner.run(
                    List.of("first", "second", "third"), key -> {
                        if ("tenant-one".equals(TenantContext.getTenantId())
                                && "COMPANY-A".equals(TenantContext.getCompanyId())
                                && Long.valueOf(7).equals(TenantContext.getUserId())) seenContext.incrementAndGet();
                        entered.countDown();
                        if (entered.getCount() == 0) release.countDown();
                        assertTrue(entered.await(2, TimeUnit.SECONDS));
                        assertTrue(release.await(2, TimeUnit.SECONDS));
                        return ControlledToolResult.builder().success(true).summary(key).build();
                    });
            assertEquals(List.of("first", "second", "third"), List.copyOf(results.keySet()));
            assertEquals(3, seenContext.get());
            TenantContext.clear();
            runner.run(List.of("one", "two"), key -> {
                assertEquals(null, TenantContext.getTenantId());
                assertEquals(null, TenantContext.getUserId());
                return ControlledToolResult.builder().success(true).build();
            });
        } finally {
            release.countDown();
            TenantContext.clear();
            runner.shutdown();
        }
    }

    @Test
    void oneFailureCancelsWholeStageWithoutReturningPartialResults() {
        ControlledQueryBranchRunner runner = new ControlledQueryBranchRunner();
        try {
            ControlledQueryBranchRunner.BranchFailure failure = assertThrows(
                    ControlledQueryBranchRunner.BranchFailure.class,
                    () -> runner.run(List.of("good", "bad"), key -> {
                        if ("bad".equals(key)) throw new SecurityException("revoked");
                        Thread.sleep(5000);
                        return ControlledToolResult.builder().success(true).build();
                    }));
            assertTrue(failure.getCause() instanceof SecurityException);
            assertEquals(2, failure.logs().size());
        } finally {
            runner.shutdown();
        }
    }
}
