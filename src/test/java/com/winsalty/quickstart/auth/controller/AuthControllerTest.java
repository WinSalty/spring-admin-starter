package com.winsalty.quickstart.auth.controller;

import com.winsalty.quickstart.auth.dto.RegisterRequest;
import com.winsalty.quickstart.auth.dto.RegisterVerifyLinkRequest;
import com.winsalty.quickstart.auth.service.AuthService;
import com.winsalty.quickstart.auth.service.support.AuthRateLimitService;
import com.winsalty.quickstart.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 认证控制器测试。
 * 覆盖注册入口与注册开关、限流、账号创建发信服务之间的调用边界。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
class AuthControllerTest {

    private static final String USERNAME = "new-user";
    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "123456";
    private static final String TOKEN = "mail-token";
    private static final String VERIFY_BASE_URL = "http://localhost:5173";
    private static final String CLIENT_IP = "127.0.0.1";

    @Test
    void registerRejectsWhenRegisterDisabled() {
        AuthService authService = mock(AuthService.class);
        AuthRateLimitService authRateLimitService = mock(AuthRateLimitService.class);
        AuthController controller = new AuthController(authService, authRateLimitService);
        ReflectionTestUtils.setField(controller, "registerEnabled", false);

        assertThrows(BusinessException.class, () -> controller.register(registerRequest(), servletRequest()));

        verifyNoInteractions(authService, authRateLimitService);
    }

    @Test
    void registerChecksRateLimitBeforeCreatingPendingAccountWhenEnabled() {
        AuthService authService = mock(AuthService.class);
        AuthRateLimitService authRateLimitService = mock(AuthRateLimitService.class);
        AuthController controller = new AuthController(authService, authRateLimitService);
        ReflectionTestUtils.setField(controller, "registerEnabled", true);
        ReflectionTestUtils.setField(controller, "registerVerifyLinkBaseUrl", VERIFY_BASE_URL);
        RegisterRequest request = registerRequest();

        controller.register(request, servletRequest());

        verify(authRateLimitService).checkRegisterVerifyCode(eq(EMAIL), eq(CLIENT_IP));
        verify(authService).register(request, VERIFY_BASE_URL);
    }

    @Test
    void registerVerifyLinkActivatesAccountWhenEnabled() {
        AuthService authService = mock(AuthService.class);
        AuthRateLimitService authRateLimitService = mock(AuthRateLimitService.class);
        AuthController controller = new AuthController(authService, authRateLimitService);
        ReflectionTestUtils.setField(controller, "registerEnabled", true);

        controller.registerVerifyLink(linkRequest());

        verify(authService).verifyRegisterEmail(EMAIL, TOKEN);
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(USERNAME);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        return request;
    }

    private MockHttpServletRequest servletRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(CLIENT_IP);
        return request;
    }

    private RegisterVerifyLinkRequest linkRequest() {
        RegisterVerifyLinkRequest request = new RegisterVerifyLinkRequest();
        request.setEmail(EMAIL);
        request.setToken(TOKEN);
        return request;
    }
}
