package com.finex.auth.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;
import java.util.UUID;

@Slf4j
@Component
public class TemplateSaveTraceInterceptor implements HandlerInterceptor {

    public static final String TRACE_HEADER = "X-Template-Save-Trace-Id";
    public static final String TRACE_ATTRIBUTE = "templateSaveTraceId";
    private static final String TRACE_START_ATTRIBUTE = "templateSaveTraceStartedAt";
    private static final String TEMPLATE_SAVE_PATH_PREFIX = "/auth/process-management/templates";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!isTemplateSaveRequest(request)) {
            return true;
        }

        String traceId = resolveOrCreateTraceId(request.getHeader(TRACE_HEADER));
        request.setAttribute(TRACE_ATTRIBUTE, traceId);
        request.setAttribute(TRACE_START_ATTRIBUTE, System.nanoTime());
        response.setHeader(TRACE_HEADER, traceId);

        log.info(
                "[TemplateSaveTrace][{}][auth] request start method={} uri={} userId={} username={}",
                traceId,
                request.getMethod(),
                request.getRequestURI(),
                request.getAttribute("currentUserId"),
                request.getAttribute("currentUsername")
        );
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!isTemplateSaveRequest(request)) {
            return;
        }

        String traceId = resolveOrCreateTraceId((String) request.getAttribute(TRACE_ATTRIBUTE));
        response.setHeader(TRACE_HEADER, traceId);

        long startedAt = request.getAttribute(TRACE_START_ATTRIBUTE) instanceof Long value ? value : System.nanoTime();
        long costMs = elapsedMillis(startedAt);
        if (ex != null) {
            log.error(
                    "[TemplateSaveTrace][{}][auth] request failed status={} costMs={} message={}",
                    traceId,
                    response.getStatus(),
                    costMs,
                    ex.getMessage(),
                    ex
            );
            return;
        }
        log.info(
                "[TemplateSaveTrace][{}][auth] request finish status={} costMs={}",
                traceId,
                response.getStatus(),
                costMs
        );
    }

    private boolean isTemplateSaveRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri == null || !requestUri.startsWith(TEMPLATE_SAVE_PATH_PREFIX)) {
            return false;
        }
        String method = request.getMethod();
        if (method == null) {
            return false;
        }
        String normalizedMethod = method.toUpperCase(Locale.ROOT);
        return "POST".equals(normalizedMethod) || "PUT".equals(normalizedMethod);
    }

    private String resolveOrCreateTraceId(String currentTraceId) {
        if (currentTraceId != null && !currentTraceId.isBlank()) {
            return currentTraceId.trim();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }
}
