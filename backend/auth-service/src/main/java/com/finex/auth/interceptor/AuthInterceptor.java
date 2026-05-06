package com.finex.auth.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 简单鉴权拦截器。
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

        request.setAttribute("currentUserId", JwtUtil.getUserId(token));
        request.setAttribute("currentUsername", JwtUtil.getUsername(token));
        return true;
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
