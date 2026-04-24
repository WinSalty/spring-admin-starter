package com.winsalty.quickstart.auth.service.impl;

import com.winsalty.quickstart.auth.dto.LoginRequest;
import com.winsalty.quickstart.auth.dto.RegisterRequest;
import com.winsalty.quickstart.auth.entity.UserEntity;
import com.winsalty.quickstart.auth.mapper.UserMapper;
import com.winsalty.quickstart.auth.security.JwtTokenProvider;
import com.winsalty.quickstart.auth.service.AuthSessionService;
import com.winsalty.quickstart.auth.service.support.RegisterVerificationService;
import com.winsalty.quickstart.common.constant.CommonStatusConstants;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.permission.mapper.PermissionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证服务注册流程测试。
 * 覆盖注册提交时创建待激活账号、重复提交刷新待激活账号、激活链接和待激活账号登录拦截。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
class AuthServiceImplTest {

    private static final long USER_ID = 100L;
    private static final String USERNAME = "new-user";
    private static final String EMAIL = "test@example.com";
    private static final String VERIFY_BASE_URL = "http://localhost:5173";
    private static final String PASSWORD = "123456";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final String TOKEN = "mail-token";
    private static final long DEFAULT_VIEWER_ROLE_ID = 2L;

    @Test
    void registerRejectsWhenUsernameExists() {
        UserMapper userMapper = mock(UserMapper.class);
        RegisterVerificationService registerVerificationService = mock(RegisterVerificationService.class);
        AuthServiceImpl service = newService(userMapper, registerVerificationService, passwordEncoder());
        when(userMapper.findByUsername(USERNAME)).thenReturn(activeUser(USERNAME, "other@example.com"));

        assertThrows(BusinessException.class, () -> service.register(registerRequest(), VERIFY_BASE_URL));

        verify(registerVerificationService, never()).sendVerificationLink(EMAIL, VERIFY_BASE_URL);
    }

    @Test
    void registerRejectsWhenEmailExists() {
        UserMapper userMapper = mock(UserMapper.class);
        RegisterVerificationService registerVerificationService = mock(RegisterVerificationService.class);
        AuthServiceImpl service = newService(userMapper, registerVerificationService, passwordEncoder());
        when(userMapper.findByEmail(EMAIL)).thenReturn(activeUser("other-user", EMAIL));

        assertThrows(BusinessException.class, () -> service.register(registerRequest(), VERIFY_BASE_URL));

        verify(registerVerificationService, never()).sendVerificationLink(EMAIL, VERIFY_BASE_URL);
    }

    @Test
    void registerCreatesPendingAccountAndSendsActivationMailWhenAvailable() {
        UserMapper userMapper = mock(UserMapper.class);
        RegisterVerificationService registerVerificationService = mock(RegisterVerificationService.class);
        AuthServiceImpl service = newService(userMapper, registerVerificationService, passwordEncoder());
        doAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(USER_ID);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));

        service.register(registerRequest(), VERIFY_BASE_URL);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper).insert(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();
        assertEquals(USERNAME, savedUser.getUsername());
        assertEquals(EMAIL, savedUser.getEmail());
        assertEquals(CommonStatusConstants.PENDING, savedUser.getStatus());
        assertEquals(ENCODED_PASSWORD, savedUser.getPassword());
        verify(userMapper).insertUserRole(USER_ID, DEFAULT_VIEWER_ROLE_ID);
        verify(registerVerificationService).sendVerificationLink(EMAIL, VERIFY_BASE_URL);
    }

    @Test
    void registerRefreshesPendingAccountAndResendsActivationMail() {
        UserMapper userMapper = mock(UserMapper.class);
        RegisterVerificationService registerVerificationService = mock(RegisterVerificationService.class);
        AuthServiceImpl service = newService(userMapper, registerVerificationService, passwordEncoder());
        UserEntity pendingUser = pendingUser();
        when(userMapper.findByUsername(USERNAME)).thenReturn(pendingUser);
        when(userMapper.findByEmail(EMAIL)).thenReturn(pendingUser);

        service.register(registerRequest(), VERIFY_BASE_URL);

        verify(userMapper).updatePendingRegistration(USER_ID, ENCODED_PASSWORD);
        verify(userMapper, never()).insert(any(UserEntity.class));
        verify(userMapper, never()).insertUserRole(eq(USER_ID), eq(DEFAULT_VIEWER_ROLE_ID));
        verify(registerVerificationService).sendVerificationLink(EMAIL, VERIFY_BASE_URL);
    }

    @Test
    void resendRegisterVerifyMailSendsActivationMailForPendingAccount() {
        UserMapper userMapper = mock(UserMapper.class);
        RegisterVerificationService registerVerificationService = mock(RegisterVerificationService.class);
        AuthServiceImpl service = newService(userMapper, registerVerificationService, passwordEncoder());
        when(userMapper.findByEmail(EMAIL)).thenReturn(pendingUser());

        service.resendRegisterVerifyMail(EMAIL, VERIFY_BASE_URL);

        verify(registerVerificationService).sendVerificationLink(EMAIL, VERIFY_BASE_URL);
    }

    @Test
    void resendRegisterVerifyMailRejectsActivatedAccount() {
        UserMapper userMapper = mock(UserMapper.class);
        RegisterVerificationService registerVerificationService = mock(RegisterVerificationService.class);
        AuthServiceImpl service = newService(userMapper, registerVerificationService, passwordEncoder());
        when(userMapper.findByEmail(EMAIL)).thenReturn(activeUser(USERNAME, EMAIL));

        assertThrows(BusinessException.class, () -> service.resendRegisterVerifyMail(EMAIL, VERIFY_BASE_URL));

        verify(registerVerificationService, never()).sendVerificationLink(EMAIL, VERIFY_BASE_URL);
    }

    @Test
    void verifyRegisterEmailActivatesPendingAccount() {
        UserMapper userMapper = mock(UserMapper.class);
        RegisterVerificationService registerVerificationService = mock(RegisterVerificationService.class);
        AuthServiceImpl service = newService(userMapper, registerVerificationService, passwordEncoder());
        when(userMapper.findByEmail(EMAIL)).thenReturn(pendingUser());
        when(userMapper.activatePendingByEmail(EMAIL)).thenReturn(1);

        service.verifyRegisterEmail(EMAIL, TOKEN);

        verify(registerVerificationService).verifyLink(EMAIL, TOKEN);
        verify(userMapper).activatePendingByEmail(EMAIL);
    }

    @Test
    void loginRejectsPendingAccount() {
        UserMapper userMapper = mock(UserMapper.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        RegisterVerificationService registerVerificationService = mock(RegisterVerificationService.class);
        AuthServiceImpl service = newService(userMapper, registerVerificationService, passwordEncoder);
        UserEntity user = pendingUser();
        user.setPassword(ENCODED_PASSWORD);
        when(userMapper.findByUsernameOrEmail(USERNAME)).thenReturn(user);
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.login(loginRequest()));
    }

    private AuthServiceImpl newService(UserMapper userMapper,
                                       RegisterVerificationService registerVerificationService,
                                       BCryptPasswordEncoder passwordEncoder) {
        return new AuthServiceImpl(
                userMapper,
                mock(PermissionMapper.class),
                mock(JwtTokenProvider.class),
                passwordEncoder,
                mock(AuthSessionService.class),
                registerVerificationService
        );
    }

    private BCryptPasswordEncoder passwordEncoder() {
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
        return passwordEncoder;
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(USERNAME);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        return request;
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername(USERNAME);
        request.setPassword(PASSWORD);
        return request;
    }

    private UserEntity pendingUser() {
        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setUsername(USERNAME);
        user.setEmail(EMAIL);
        user.setStatus(CommonStatusConstants.PENDING);
        return user;
    }

    private UserEntity activeUser(String username, String email) {
        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setUsername(username);
        user.setEmail(email);
        user.setStatus(CommonStatusConstants.ACTIVE);
        return user;
    }
}
