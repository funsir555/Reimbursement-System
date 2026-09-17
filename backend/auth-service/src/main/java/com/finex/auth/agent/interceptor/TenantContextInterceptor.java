package com.finex.auth.agent.interceptor;

import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.agent.context.TraceContext;
import com.finex.auth.entity.User;
import com.finex.auth.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户上下文拦截器
 * 在每个请求开始时设置租户上下文，请求结束时清理
 */
@Slf4j
@Component
public class TenantContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // 初始化追踪ID
            String traceId = request.getHeader("X-Trace-Id");
            if (traceId == null || traceId.isEmpty()) {
                traceId = TraceContext.generateTraceId();
            }
            TraceContext.setTraceId(traceId);

            // 从 Spring Security 获取当前用户
            Long userId = SecurityUtils.getCurrentUserId();

            if (userId != null) {
                // Demo 模式：固定租户
                TenantContext.setTenantId("default");

                // 获取用户的公司ID
                User user = SecurityUtils.getCurrentUser();
                if (user != null && user.getCompanyId() != null) {
                    try {
                        // companyId 是 String 类型，需要转换为 Long
                        Long companyId = Long.parseLong(user.getCompanyId());
                        TenantContext.setCompanyId(companyId);
                    } catch (NumberFormatException e) {
                        // 如果转换失败，使用默认值
                        TenantContext.setCompanyId(1L);
                    }
                } else {
                    // 回退到默认公司ID
                    TenantContext.setCompanyId(1L);
                }

                TenantContext.setUserId(userId);

                log.debug("设置租户上下文: tenantId=default, companyId={}, userId={}, traceId={}",
                        TenantContext.getCompanyId(), userId, traceId);
            } else {
                log.debug("未登录用户，跳过租户上下文设置");
            }

            // 将 traceId 添加到响应头
            response.setHeader("X-Trace-Id", traceId);

        } catch (Exception e) {
            log.error("设置租户上下文失败", e);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 清理线程上下文
        TenantContext.clear();
        TraceContext.clear();

        log.debug("清理租户上下文");
    }
}
