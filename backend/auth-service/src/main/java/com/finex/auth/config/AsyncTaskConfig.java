package com.finex.auth.config;

import com.finex.auth.agent.context.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

@Configuration
@Slf4j
public class AsyncTaskConfig {

    @Value("${finex.async.core-pool-size:4}")
    private int corePoolSize;

    @Value("${finex.async.max-pool-size:8}")
    private int maxPoolSize;

    @Value("${finex.async.queue-capacity:200}")
    private int queueCapacity;

    @Value("${finex.async.thread-name-prefix:finex-async-}")
    private String threadNamePrefix;

    @Value("${finex.async.await-termination-seconds:30}")
    private int awaitTerminationSeconds;

    @Bean("finexAsyncExecutor")
    public Executor finexAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.setTaskDecorator(task -> {
            String traceId = TraceContext.currentOrNull();
            return () -> {
                String previousTraceId = TraceContext.currentOrNull();
                String previousMdcTraceId = MDC.get(RequestTraceFilter.MDC_KEY);
                long startedAt = System.nanoTime();
                String outcome = "success";
                try {
                    if (traceId != null) {
                        TraceContext.setTraceId(traceId);
                        MDC.put(RequestTraceFilter.MDC_KEY, traceId);
                    }
                    task.run();
                    if (task instanceof Future<?> future && future.isDone()) {
                        try {
                            future.get();
                        } catch (CancellationException cancelled) {
                            outcome = "cancelled";
                        } catch (ExecutionException failed) {
                            outcome = "failure";
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            outcome = "interrupted";
                        }
                    }
                } catch (RuntimeException | Error failure) {
                    outcome = "failure";
                    throw failure;
                } finally {
                    log.info("async_task_completed outcome={} queue_depth={} duration_ms={} trace_id={}",
                            outcome,
                            executor.getThreadPoolExecutor().getQueue().size(),
                            (System.nanoTime() - startedAt) / 1_000_000, traceId);
                    if (previousTraceId == null) {
                        TraceContext.clear();
                    } else {
                        TraceContext.setTraceId(previousTraceId);
                    }
                    if (previousMdcTraceId == null) {
                        MDC.remove(RequestTraceFilter.MDC_KEY);
                    } else {
                        MDC.put(RequestTraceFilter.MDC_KEY, previousMdcTraceId);
                    }
                }
            };
        });
        executor.initialize();
        return executor;
    }

    @Bean("finWorkStreamExecutor")
    public Executor finWorkStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(32);
        executor.setThreadNamePrefix("finwork-stream-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
