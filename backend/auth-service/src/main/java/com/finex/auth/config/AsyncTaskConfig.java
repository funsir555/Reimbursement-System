package com.finex.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
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
        executor.initialize();
        return executor;
    }
}
