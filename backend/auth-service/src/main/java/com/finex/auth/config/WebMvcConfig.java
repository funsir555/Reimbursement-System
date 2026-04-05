package com.finex.auth.config;

import com.finex.auth.interceptor.AuthInterceptor;
import com.finex.auth.interceptor.TemplateSaveTraceInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final TemplateSaveTraceInterceptor templateSaveTraceInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/auth/**")
                .excludePathPatterns("/auth/login", "/auth/test", "/auth/expenses/attachments/*/content");
        registry.addInterceptor(templateSaveTraceInterceptor)
                .addPathPatterns("/auth/process-management/templates", "/auth/process-management/templates/**");
    }
}
