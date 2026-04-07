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
 * 绠€鍗曢壌鏉冩嫤鎴櫒
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorization = request.getHeader("Authorization");
        String token = extractToken(authorization);
        if (!JwtUtil.verify(token)) {
            writeUnauthorized(response);
            return false;
        }

        request.setAttribute("currentUserId", JwtUtil.getUserId(token));
        request.setAttribute("currentUsername", JwtUtil.getUsername(token));
        return true;
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

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Result.unauthorized("鏈櫥褰曟垨鐧诲綍宸茶繃鏈?")));
    }
}
