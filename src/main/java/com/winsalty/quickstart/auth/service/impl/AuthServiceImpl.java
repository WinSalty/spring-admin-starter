package com.winsalty.quickstart.auth.service.impl;

import com.winsalty.quickstart.auth.dto.LoginRequest;
import com.winsalty.quickstart.auth.dto.RefreshTokenRequest;
import com.winsalty.quickstart.auth.dto.RegisterRequest;
import com.winsalty.quickstart.auth.entity.UserEntity;
import com.winsalty.quickstart.auth.mapper.UserMapper;
import com.winsalty.quickstart.auth.security.JwtTokenProvider;
import com.winsalty.quickstart.auth.security.TokenPayload;
import com.winsalty.quickstart.auth.service.AuthService;
import com.winsalty.quickstart.auth.service.AuthSessionService;
import com.winsalty.quickstart.auth.vo.LoginResponse;
import com.winsalty.quickstart.auth.vo.ProfileResponse;
import com.winsalty.quickstart.auth.vo.RefreshTokenResponse;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.permission.mapper.PermissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 认证服务实现。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;

    public AuthServiceImpl(UserMapper userMapper,
                           PermissionMapper permissionMapper,
                           JwtTokenProvider jwtTokenProvider,
                           BCryptPasswordEncoder passwordEncoder,
                           AuthSessionService authSessionService) {
        this.userMapper = userMapper;
        this.permissionMapper = permissionMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.authSessionService = authSessionService;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        UserEntity user = userMapper.findByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(4004, "用户名或密码错误");
        }
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException(4005, "当前账号不可用");
        }
        String roleCode = permissionMapper.findRoleCodeByUserId(user.getId());
        if (!StringUtils.hasText(roleCode)) {
            throw new BusinessException(4006, "当前账号未分配角色");
        }
        String sessionId = jwtTokenProvider.generateSessionId();
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), roleCode, sessionId);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getUsername(), roleCode, sessionId);
        authSessionService.createSession(sessionId, refreshToken, jwtTokenProvider.getRefreshExpireSeconds());
        log.info("user login success, username={}, roleCode={}, sessionId={}", user.getUsername(), roleCode, sessionId);
        return new LoginResponse(accessToken, accessToken, refreshToken,
                jwtTokenProvider.getAccessExpireSeconds(), jwtTokenProvider.getRefreshExpireSeconds(), "Bearer");
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        TokenPayload payload = jwtTokenProvider.parseToken(request.getRefreshToken());
        if (!"refresh".equals(payload.getTokenType())) {
            throw new BusinessException(4011, "刷新令牌无效或已过期");
        }
        if (!authSessionService.exists(payload.getSessionId())
                || !authSessionService.matchesRefreshToken(payload.getSessionId(), request.getRefreshToken())) {
            throw new BusinessException(4012, "当前会话已失效，请重新登录");
        }
        String accessToken = jwtTokenProvider.createAccessToken(payload.getUserId(), payload.getUsername(), payload.getRoleCode(), payload.getSessionId());
        String refreshToken = jwtTokenProvider.createRefreshToken(payload.getUserId(), payload.getUsername(), payload.getRoleCode(), payload.getSessionId());
        authSessionService.refreshSession(payload.getSessionId(), refreshToken, jwtTokenProvider.getRefreshExpireSeconds());
        RefreshTokenResponse response = new RefreshTokenResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtTokenProvider.getAccessExpireSeconds());
        response.setRefreshExpiresIn(jwtTokenProvider.getRefreshExpireSeconds());
        response.setTokenType("Bearer");
        log.info("token refreshed, username={}, roleCode={}, sessionId={}", payload.getUsername(), payload.getRoleCode(), payload.getSessionId());
        return response;
    }

    @Override
    public void logout(Long userId, String sessionId) {
        authSessionService.deleteSession(sessionId);
        log.info("user logout success, userId={}, sessionId={}", userId, sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        UserEntity existedUser = userMapper.findByUsername(request.getUsername());
        if (existedUser != null) {
            throw new BusinessException(4007, "用户名已存在");
        }
        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setNickname(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus("active");
        user.setDeleted(0);
        userMapper.insert(user);
        userMapper.insertUserRole(user.getId(), 2L);
        log.info("user register success, username={}, roleCode=viewer", user.getUsername());
    }

    @Override
    public ProfileResponse getProfile(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() != null && user.getDeleted() == 1) {
            throw new BusinessException(4040, "用户不存在");
        }
        ProfileResponse response = new ProfileResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRoleCode(permissionMapper.findRoleCodeByUserId(userId));
        response.setRoleName(permissionMapper.findRoleNameByUserId(userId));
        return response;
    }
}
