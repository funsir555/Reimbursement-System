package com.finex.auth.config;

import com.finex.auth.agent.context.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AsyncTaskConfigTraceTest {

    @AfterEach
    void clear() {
        TraceContext.clear();
        MDC.clear();
    }

    @Test
    void propagatesTraceAndDoesNotLeakToNextTask() throws Exception {
        AsyncTaskConfig config = new AsyncTaskConfig();
        ReflectionTestUtils.setField(config, "corePoolSize", 1);
        ReflectionTestUtils.setField(config, "maxPoolSize", 1);
        ReflectionTestUtils.setField(config, "queueCapacity", 10);
        ReflectionTestUtils.setField(config, "threadNamePrefix", "trace-test-");
        ReflectionTestUtils.setField(config, "awaitTerminationSeconds", 1);
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.finexAsyncExecutor();
        try {
            TraceContext.setTraceId("request_12345678");
            assertThat(executor.submit(() -> {
                assertThat(MDC.get(RequestTraceFilter.MDC_KEY)).isEqualTo("request_12345678");
                return TraceContext.currentOrNull();
            }).get(5, TimeUnit.SECONDS)).isEqualTo("request_12345678");

            assertThrows(ExecutionException.class, () -> executor.submit(() -> {
                assertThat(TraceContext.currentOrNull()).isEqualTo("request_12345678");
                throw new IllegalStateException("simulated async failure");
            }).get(5, TimeUnit.SECONDS));

            TraceContext.clear();
            assertThat(executor.submit(() -> {
                assertThat(MDC.get(RequestTraceFilter.MDC_KEY)).isNull();
                return TraceContext.currentOrNull();
            }).get(5, TimeUnit.SECONDS)).isNull();
        } finally {
            executor.shutdown();
        }
    }
}
