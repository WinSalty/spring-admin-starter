package com.winsalty.quickstart.auth.config;

import com.winsalty.quickstart.auth.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * 安全配置测试。
 * 覆盖匿名公开接口匹配器，避免注册相关入口被 JWT 鉴权误拦截。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
class SecurityConfigTest {

    @Test
    void publicMatchersAllowRegisterResendVerifyMailEndpoint() {
        SecurityConfig securityConfig = new SecurityConfig(mock(JwtAuthenticationFilter.class), false);
        RequestMatcher[] matchers = ReflectionTestUtils.invokeMethod(securityConfig, "buildPublicMatchers");
        MockHttpServletRequest request = new MockHttpServletRequest("POST",
                "/api/auth/register/resend-verify-mail");
        request.setServletPath("/api/auth/register/resend-verify-mail");

        assertTrue(Arrays.stream(matchers).anyMatch(matcher -> matcher.matches(request)));
    }
}
