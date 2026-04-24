package com.winsalty.quickstart.auth.service.impl;

import com.winsalty.quickstart.auth.dto.RegisterRequest;
import com.winsalty.quickstart.auth.entity.UserEntity;
import com.winsalty.quickstart.auth.mapper.UserMapper;
import com.winsalty.quickstart.auth.security.JwtTokenProvider;
import com.winsalty.quickstart.auth.service.AuthSessionService;
import com.winsalty.quickstart.auth.service.support.RegisterVerificationService;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.permission.mapper.PermissionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证服务注册流程测试。
 * 覆盖发送注册验证码前的用户名、邮箱唯一性前置校验，避免用户填完验证码后才发现账号不可用。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
class AuthServiceImplTest {

    private static final String USERNAME = "new-user";
    private static final String EMAIL = "test@example.com";
    private static final String VERIFY_CODE = "123456";
    private static final String PASSWORD = "123456";

    @Test
    void sendRegisterVerifyCodeRejectsWhenUsernameExists() {
        UserMapper userMapper = mock(UserMapper.class);
        RegisterVerificationService registerVerificationService = mock(RegisterVerificationService.class);
        AuthServiceImpl service = newService(userMapper, registerVerificationService);
        when(userMapper.findByUsername(USERNAME)).thenReturn(new UserEntity());

        assertThrows(BusinessException.class, () -> service.sendRegisterVerifyCode(USERNAME, EMAIL));

        verify(registerVerificationService, never()).sendCode(EMAIL);
    }

    @Test
    void sendRegisterVerifyCodeRejectsWhenEmailExists() {
        UserMapper userMapper = mock(UserMapper.class);
        RegisterVerificationService registerVerificationService = mock(RegisterVerificationService.class);
        AuthServiceImpl service = newService(userMapper, registerVerificationService);
        when(userMapper.findByEmail(EMAIL)).thenReturn(new UserEntity());

        assertThrows(BusinessException.class, () -> service.sendRegisterVerifyCode(USERNAME, EMAIL));

        verify(registerVerificationService, never()).sendCode(EMAIL);
    }

    @Test
    void sendRegisterVerifyCodeSendsCodeWhenAccountAvailable() {
        UserMapper userMapper = mock(UserMapper.class);
        RegisterVerificationService registerVerificationService = mock(RegisterVerificationService.class);
        AuthServiceImpl service = newService(userMapper, registerVerificationService);

        service.sendRegisterVerifyCode(" " + USERNAME + " ", " Test@Example.com ");

        verify(userMapper).findByUsername(USERNAME);
        verify(userMapper).findByEmail(EMAIL);
        verify(registerVerificationService).sendCode(EMAIL);
    }

    @Test
    void registerDoesNotConsumeVerifyCodeWhenUsernameExists() {
        UserMapper userMapper = mock(UserMapper.class);
        RegisterVerificationService registerVerificationService = mock(RegisterVerificationService.class);
        AuthServiceImpl service = newService(userMapper, registerVerificationService);
        when(userMapper.findByUsername(USERNAME)).thenReturn(new UserEntity());

        assertThrows(BusinessException.class, () -> service.register(registerRequest()));

        verify(registerVerificationService, never()).verifyCode(EMAIL, VERIFY_CODE);
    }

    private AuthServiceImpl newService(UserMapper userMapper, RegisterVerificationService registerVerificationService) {
        return new AuthServiceImpl(
                userMapper,
                mock(PermissionMapper.class),
                mock(JwtTokenProvider.class),
                mock(BCryptPasswordEncoder.class),
                mock(AuthSessionService.class),
                registerVerificationService
        );
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(USERNAME);
        request.setEmail(EMAIL);
        request.setVerifyCode(VERIFY_CODE);
        request.setPassword(PASSWORD);
        return request;
    }
}
