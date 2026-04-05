package com.finex.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class TemplateSaveTraceFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TemplateSaveTraceFilter.class);
    private static final String TRACE_HEADER = "X-Template-Save-Trace-Id";
    private static final String TEMPLATE_SAVE_PATH_PREFIX = "/api/auth/process-management/templates";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest originalRequest = exchange.getRequest();
        if (!isTemplateSaveRequest(originalRequest)) {
            return chain.filter(exchange);
        }

        String traceId = resolveOrCreateTraceId(originalRequest.getHeaders().getFirst(TRACE_HEADER));
        long startedAt = System.nanoTime();

        ServerHttpRequest tracedRequest = originalRequest.mutate()
                .headers(headers -> headers.set(TRACE_HEADER, traceId))
                .build();
        ServerWebExchange tracedExchange = exchange.mutate().request(tracedRequest).build();
        tracedExchange.getResponse().getHeaders().set(TRACE_HEADER, traceId);

        log.info(
                "[TemplateSaveTrace][{}][gateway] request start method={} path={} query={}",
                traceId,
                tracedRequest.getMethod(),
                tracedRequest.getURI().getPath(),
                tracedRequest.getURI().getQuery()
        );

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        return chain.filter(tracedExchange)
                .doOnError(errorRef::set)
                .doFinally(signalType -> {
                    Throwable error = errorRef.get();
                    HttpStatusCode statusCode = tracedExchange.getResponse().getStatusCode();
                    String status = statusCode == null ? "NA" : String.valueOf(statusCode.value());
                    long costMs = elapsedMillis(startedAt);
                    if (error != null) {
                        log.error(
                                "[TemplateSaveTrace][{}][gateway] request failed signal={} status={} costMs={} message={}",
                                traceId,
                                signalType,
                                status,
                                costMs,
                                error.getMessage(),
                                error
                        );
                        return;
                    }
                    log.info(
                            "[TemplateSaveTrace][{}][gateway] request finish signal={} status={} costMs={}",
                            traceId,
                            signalType,
                            status,
                            costMs
                    );
                });
    }

    private boolean isTemplateSaveRequest(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        if (path == null || !path.startsWith(TEMPLATE_SAVE_PATH_PREFIX) || request.getMethod() == null) {
            return false;
        }
        String method = request.getMethod().name().toUpperCase(Locale.ROOT);
        return "POST".equals(method) || "PUT".equals(method);
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
