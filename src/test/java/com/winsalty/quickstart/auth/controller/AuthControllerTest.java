package com.winsalty.quickstart.auth.controller;

import com.winsalty.quickstart.auth.dto.RegisterVerifyCodeRequest;
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
 * 覆盖注册验证码入口与注册开关、限流和邮件发送服务之间的调用边界。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
class AuthControllerTest {

    private static final String EMAIL = "test@example.com";
    private static final String CLIENT_IP = "127.0.0.1";

    @Test
    void registerVerifyCodeRejectsWhenRegisterDisabled() {
        AuthService authService = mock(AuthService.class);
        AuthRateLimitService authRateLimitService = mock(AuthRateLimitService.class);
        AuthController controller = new AuthController(authService, authRateLimitService);
        ReflectionTestUtils.setField(controller, "registerEnabled", false);

        assertThrows(BusinessException.class, () -> controller.registerVerifyCode(request(), servletRequest()));

        verifyNoInteractions(authService, authRateLimitService);
    }

    @Test
    void registerVerifyCodeChecksRateLimitBeforeSendingMailWhenEnabled() {
        AuthService authService = mock(AuthService.class);
        AuthRateLimitService authRateLimitService = mock(AuthRateLimitService.class);
        AuthController controller = new AuthController(authService, authRateLimitService);
        ReflectionTestUtils.setField(controller, "registerEnabled", true);

        controller.registerVerifyCode(request(), servletRequest());

        verify(authRateLimitService).checkRegisterVerifyCode(eq(EMAIL), eq(CLIENT_IP));
        verify(authService).sendRegisterVerifyCode(EMAIL);
    }

    private RegisterVerifyCodeRequest request() {
        RegisterVerifyCodeRequest request = new RegisterVerifyCodeRequest();
        request.setEmail(EMAIL);
        return request;
    }

    private MockHttpServletRequest servletRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(CLIENT_IP);
        return request;
    }
}
