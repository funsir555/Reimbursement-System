package com.finex.auth.agent.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.agent.context.TraceContext;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.util.SecurityUtils;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 租户上下文拦截器
 * 在每个请求开始时设置租户上下文，请求结束时清理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantContextInterceptor implements HandlerInterceptor {

    private static final String DEFAULT_TENANT_ID = "default";

    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String traceId = TraceContext.getCurrentTraceId();
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                clearContexts();
                return writeError(response, HttpServletResponse.SC_UNAUTHORIZED, Result.unauthorized("未登录或登录已过期"));
            }

            User user = userMapper.selectById(userId);
            String companyId = user == null || user.getCompanyId() == null ? "" : user.getCompanyId().trim();
            if (user == null || !Integer.valueOf(1).equals(user.getStatus()) || companyId.isEmpty()) {
                clearContexts();
                return writeError(response, HttpServletResponse.SC_FORBIDDEN, Result.forbidden("当前用户缺少可用的公司上下文"));
            }

            TenantContext.setTenantId(DEFAULT_TENANT_ID);
            TenantContext.setCompanyId(companyId);
            TenantContext.setUserId(userId);
            response.setHeader("X-Trace-Id", traceId);
            log.debug("设置租户上下文: tenantId={}, companyId={}, userId={}, traceId={}",
                    DEFAULT_TENANT_ID, companyId, userId, traceId);
            return true;
        } catch (Exception e) {
            log.error("设置租户上下文失败: path={}", request.getRequestURI(), e);
            clearContexts();
            return writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Result.error(500, "系统异常，请稍后重试"));
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 清理线程上下文
        clearContexts();
        log.debug("清理租户上下文");
    }

    private boolean writeError(HttpServletResponse response, int status, Result<Void> result) {
        try {
            response.setStatus(status);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(result));
        } catch (Exception writeError) {
            log.error("写入 Agent 上下文拒绝响应失败", writeError);
        }
        return false;
    }

    private void clearContexts() {
        TenantContext.clear();
        TraceContext.clear();
    }
}
