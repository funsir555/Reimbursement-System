package com.finex.auth.config;

import com.finex.auth.agent.context.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 为每次 HTTP 请求建立可信的关联 ID，并在请求结束时清理线程状态。 */
@Slf4j
public class RequestTraceFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String previousTraceId = TraceContext.currentOrNull();
        String previousMdcTraceId = MDC.get(MDC_KEY);
        String supplied = request.getHeader(HEADER_NAME);
        String traceId = validTraceId(supplied) ? supplied : TraceContext.generateTraceId();
        long startedAt = System.nanoTime();
        TraceContext.setTraceId(traceId);
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER_NAME, traceId);
        String outcome = "success";
        try {
            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException | Error failure) {
            outcome = "failure";
            throw failure;
        } finally {
            log.info("http_request_completed method={} path={} status={} outcome={} duration_ms={} trace_id={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), outcome,
                    (System.nanoTime() - startedAt) / 1_000_000, traceId);
            if (previousTraceId == null) {
                TraceContext.clear();
            } else {
                TraceContext.setTraceId(previousTraceId);
            }
            if (previousMdcTraceId == null) {
                MDC.remove(MDC_KEY);
            } else {
                MDC.put(MDC_KEY, previousMdcTraceId);
            }
        }
    }

    private boolean validTraceId(String value) {
        return value != null && value.length() >= 8 && value.length() <= 64
                && value.matches("[A-Za-z0-9._-]+");
    }
}
