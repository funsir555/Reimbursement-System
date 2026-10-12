package com.finex.auth.config;

import com.finex.auth.agent.context.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestTraceFilterTest {

    private final RequestTraceFilter filter = new RequestTraceFilter();

    @AfterEach
    void clear() {
        TraceContext.clear();
        MDC.clear();
    }

    @Test
    void acceptsSafeCorrelationIdAndClearsThreadAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/profile");
        request.addHeader(RequestTraceFilter.HEADER_NAME, "request_12345678");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> {
            seen.set(TraceContext.getCurrentTraceId());
            assertThat(MDC.get(RequestTraceFilter.MDC_KEY)).isEqualTo("request_12345678");
        });

        assertThat(seen.get()).isEqualTo("request_12345678");
        assertThat(response.getHeader(RequestTraceFilter.HEADER_NAME)).isEqualTo("request_12345678");
        assertThat(TraceContext.currentOrNull()).isNull();
        assertThat(MDC.get(RequestTraceFilter.MDC_KEY)).isNull();
    }

    @Test
    void rejectsHeaderInjectionAndStillCleansOnFailure() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/agent/chat");
        request.addHeader(RequestTraceFilter.HEADER_NAME, "bad\r\nheader");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            filter.doFilter(request, response, (req, res) -> {
                assertThat(TraceContext.getCurrentTraceId()).startsWith("trace_");
                throw new IllegalStateException("simulated failure");
            });
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage()).isEqualTo("simulated failure");
        }

        assertThat(response.getHeader(RequestTraceFilter.HEADER_NAME)).startsWith("trace_");
        assertThat(TraceContext.currentOrNull()).isNull();
        assertThat(MDC.get(RequestTraceFilter.MDC_KEY)).isNull();
    }
}
