package com.finex.auth.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.util.SecurityUtils;
import com.finex.common.JwtUtil;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 简单鉴权拦截器
 *
 * 职责：
 * 1. 验证 JWT Token 有效性
 * 2. 提取用户信息并存储到 request attribute（兼容现有代码）
 * 3. 设置用户信息到 SecurityUtils ThreadLocal（支持 Agent Chat）
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String ATTACHMENT_PREVIEW_TOKEN_PARAM = "token";

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = resolveToken(request);
        if (!JwtUtil.verify(token)) {
            writeUnauthorized(response);
            return false;
        }

        Long userId = JwtUtil.getUserId(token);
        String username = JwtUtil.getUsername(token);

        // 存储到 request attribute（保持现有代码兼容性）
        request.setAttribute("currentUserId", userId);
        request.setAttribute("currentUsername", username);

        // 设置到 SecurityUtils ThreadLocal（支持 Agent Chat 等新功能）
        SecurityUtils.setCurrentUserId(userId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) throws Exception {
        // 清理 ThreadLocal，避免内存泄漏
        SecurityUtils.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String headerToken = extractToken(request.getHeader("Authorization"));
        if (!headerToken.isBlank()) {
            return headerToken;
        }
        if (supportsQueryToken(request)) {
            String queryToken = request.getParameter(ATTACHMENT_PREVIEW_TOKEN_PARAM);
            return queryToken == null ? "" : queryToken.trim();
        }
        return "";
    }

    private String extractToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return "";
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }

    private boolean supportsQueryToken(HttpServletRequest request) {
        String method = request.getMethod();
        if (method == null || !"GET".equalsIgnoreCase(method)) {
            return false;
        }
        String path = request.getRequestURI();
        return path != null
                && path.contains("/auth/expenses/")
                && path.contains("/attachments/")
                && path.endsWith("/content");
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Result.unauthorized("未登录或登录已过期")));
    }
}
