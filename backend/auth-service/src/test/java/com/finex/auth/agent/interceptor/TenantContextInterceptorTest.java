package com.finex.auth.agent.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantContextInterceptorTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TenantContextInterceptor interceptor = new TenantContextInterceptor(userMapper, objectMapper);

    @AfterEach
    void clearContext() {
        SecurityUtils.clear();
        TenantContext.clear();
    }

    @Test
    void rejectsUnauthenticatedRequestWithoutQueryingUser() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean accepted = interceptor.preHandle(request(), response, new Object());

        assertThat(accepted).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(resultCode(response)).isEqualTo(401);
        verify(userMapper, never()).selectById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void propagatesNonNumericCompanyCodeWithOneIdentityQuery() {
        SecurityUtils.setCurrentUserId(7L);
        User user = activeUser("GROUP_HQ");
        when(userMapper.selectById(7L)).thenReturn(user);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean accepted = interceptor.preHandle(request(), response, new Object());

        assertThat(accepted).isTrue();
        assertThat(TenantContext.getEffectiveTenantId()).isEqualTo("default");
        assertThat(TenantContext.getEffectiveCompanyId()).isEqualTo("GROUP_HQ");
        assertThat(TenantContext.getUserId()).isEqualTo(7L);
        assertThat(response.getHeader("X-Trace-Id")).isNotBlank();
        verify(userMapper).selectById(7L);

        interceptor.afterCompletion(request(), response, new Object(), null);
        assertThat(TenantContext.getCompanyId()).isNull();
    }

    @Test
    void rejectsDisabledOrCompanylessUser() throws Exception {
        SecurityUtils.setCurrentUserId(7L);
        User disabled = activeUser("GROUP_HQ");
        disabled.setStatus(0);
        when(userMapper.selectById(7L)).thenReturn(disabled);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request(), response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(resultCode(response)).isEqualTo(403);
        assertThat(TenantContext.getCompanyId()).isNull();
    }

    @Test
    void hidesIdentityLookupFailureAndClearsContext() throws Exception {
        SecurityUtils.setCurrentUserId(7L);
        when(userMapper.selectById(7L)).thenThrow(new IllegalStateException("database details"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request(), response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(resultCode(response)).isEqualTo(500);
        assertThat(response.getContentAsString()).doesNotContain("database details");
        assertThat(TenantContext.getCompanyId()).isNull();
    }

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest("POST", "/auth/agent/chat");
    }

    private User activeUser(String companyId) {
        User user = new User();
        user.setId(7L);
        user.setStatus(1);
        user.setCompanyId(companyId);
        return user;
    }

    private int resultCode(MockHttpServletResponse response) throws Exception {
        JsonNode json = objectMapper.readTree(response.getContentAsByteArray());
        return json.path("code").asInt();
    }
}
