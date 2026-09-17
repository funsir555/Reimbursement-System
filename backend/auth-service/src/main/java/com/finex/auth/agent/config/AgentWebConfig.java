package com.finex.auth.agent.config;

import com.finex.auth.agent.interceptor.TenantContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Agent Web 配置
 * 注册租户上下文拦截器
 */
@Configuration
@RequiredArgsConstructor
public class AgentWebConfig implements WebMvcConfigurer {

    private final TenantContextInterceptor tenantContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantContextInterceptor)
                .addPathPatterns("/api/agent/**")
                .order(1);  // 设置较高优先级
    }
}
