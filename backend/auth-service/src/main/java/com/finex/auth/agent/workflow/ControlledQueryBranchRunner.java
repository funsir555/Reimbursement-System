package com.finex.auth.agent.workflow;

import com.finex.auth.agent.context.AgentExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PreDestroy;

/** Runs one controlled fan-out without borrowing the outer Agent run executor. */
@Component
public class ControlledQueryBranchRunner {
    private final long branchTimeoutNanos;
    private final long stageTimeoutNanos;

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 8, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(32), task -> {
                Thread thread = new Thread(task, "agent-query-branch");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());

    public ControlledQueryBranchRunner() {
        this(TimeUnit.SECONDS.toNanos(45), TimeUnit.SECONDS.toNanos(50));
    }

    ControlledQueryBranchRunner(long branchTimeoutNanos, long stageTimeoutNanos) {
        this.branchTimeoutNanos = branchTimeoutNanos;
        this.stageTimeoutNanos = stageTimeoutNanos;
    }

    public record BranchResult(ControlledToolResult result, LocalDateTime startedAt,
                               LocalDateTime finishedAt) { }

    public record BranchLog(String status, LocalDateTime startedAt, LocalDateTime finishedAt) { }

    public static final class BranchFailure extends Exception {
        private final Map<String, BranchLog> logs;

        BranchFailure(Throwable cause, Map<String, BranchLog> logs) {
            super(cause == null ? "并行查询失败" : cause.getMessage(), cause);
            this.logs = logs;
        }

        public Map<String, BranchLog> logs() { return logs; }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    public Map<String, BranchResult> run(List<String> keys, BranchTask task) throws Exception {
        if (keys.size() < 2 || keys.size() > 4)
            throw new IllegalArgumentException("并行区只允许 2 至 4 个查询分支");
        AgentExecutionContext context = AgentExecutionContext.capture();
        CompletionService<Map.Entry<String, BranchResult>> completions = new ExecutorCompletionService<>(executor);
        List<Future<Map.Entry<String, BranchResult>>> futures = new ArrayList<>();
        Map<String, BranchLog> logs = new ConcurrentHashMap<>();
        Map<String, Long> branchStarts = new ConcurrentHashMap<>();
        long stageDeadline = System.nanoTime() + stageTimeoutNanos;
        try {
            for (String key : keys) {
                Callable<Map.Entry<String, BranchResult>> job = () -> {
                    AgentExecutionContext.clear();
                    try {
                        context.restore();
                        LocalDateTime startedAt = LocalDateTime.now();
                        logs.put(key, new BranchLog("RUNNING", startedAt, null));
                        long startedNanos = System.nanoTime();
                        branchStarts.put(key, startedNanos);
                        try {
                            ControlledToolResult result = task.execute(key);
                            if (System.nanoTime() - startedNanos > branchTimeoutNanos)
                                throw new IllegalStateException("并行查询分支超时");
                            LocalDateTime finishedAt = LocalDateTime.now();
                            logs.put(key, new BranchLog("SUCCESS", startedAt, finishedAt));
                            return Map.entry(key, new BranchResult(result, startedAt, finishedAt));
                        } catch (Exception failure) {
                            logs.put(key, new BranchLog("FAILED", startedAt, LocalDateTime.now()));
                            throw failure;
                        }
                    } finally {
                        AgentExecutionContext.clear();
                    }
                };
                futures.add(completions.submit(job));
            }
            Map<String, BranchResult> completed = new LinkedHashMap<>();
            while (completed.size() < keys.size()) {
                long now = System.nanoTime();
                if (now >= stageDeadline) throw new IllegalStateException("并行查询阶段超时");
                for (Map.Entry<String, Long> start : branchStarts.entrySet()) {
                    if (!completed.containsKey(start.getKey())
                            && "RUNNING".equals(logs.get(start.getKey()).status())
                            && now - start.getValue() >= branchTimeoutNanos)
                        throw new IllegalStateException("并行查询分支超时");
                }
                long remaining = Math.min(stageDeadline - now, TimeUnit.MILLISECONDS.toNanos(100));
                Future<Map.Entry<String, BranchResult>> next = completions.poll(remaining, TimeUnit.NANOSECONDS);
                if (next == null) continue;
                Map.Entry<String, BranchResult> value = next.get();
                completed.put(value.getKey(), value.getValue());
            }
            Map<String, BranchResult> ordered = new LinkedHashMap<>();
            keys.forEach(key -> ordered.put(key, completed.get(key)));
            return ordered;
        } catch (ExecutionException | RuntimeException failed) {
            Throwable cause = failed instanceof ExecutionException ? failed.getCause() : failed;
            throw new BranchFailure(cause, snapshot(keys, logs));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new BranchFailure(interrupted, snapshot(keys, logs));
        } finally {
            futures.forEach(future -> future.cancel(true));
        }
    }

    private static Map<String, BranchLog> snapshot(List<String> keys, Map<String, BranchLog> logs) {
        Map<String, BranchLog> result = new LinkedHashMap<>();
        for (String key : keys) {
            BranchLog log = logs.get(key);
            result.put(key, log == null ? new BranchLog("CANCELLED", null, LocalDateTime.now())
                    : "RUNNING".equals(log.status())
                    ? new BranchLog("CANCELLED", log.startedAt(), LocalDateTime.now()) : log);
        }
        return result;
    }

    @FunctionalInterface
    public interface BranchTask {
        ControlledToolResult execute(String nodeKey) throws Exception;
    }
}
