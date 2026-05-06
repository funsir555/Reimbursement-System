package com.finex.auth.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.common.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthInterceptorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void setUp() {
        authInterceptor = new AuthInterceptor(objectMapper);
    }

    @Test
    void preHandleAllowsAttachmentPreviewUsingQueryToken() throws Exception {
        String token = JwtUtil.generateToken(8L, "tester");
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/auth/expenses/DOC-001/attachments/ATT-001/content"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setParameter("token", token);

        boolean allowed = authInterceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        assertEquals(8L, request.getAttribute("currentUserId"));
        assertEquals("tester", request.getAttribute("currentUsername"));
    }

    @Test
    void preHandleWritesUtf8UnauthorizedMessageWhenTokenIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/auth/expenses/DOC-001/attachments/ATT-001/content"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = authInterceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        assertEquals(
                "未登录或登录已过期",
                objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8)).path("message").asText()
        );
    }
}
