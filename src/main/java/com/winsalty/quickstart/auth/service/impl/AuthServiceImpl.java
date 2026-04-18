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
import com.winsalty.quickstart.auth.service.support.RegisterVerificationService;
import com.winsalty.quickstart.auth.vo.LoginResponse;
import com.winsalty.quickstart.auth.vo.ProfileResponse;
import com.winsalty.quickstart.auth.vo.RefreshTokenResponse;
import com.winsalty.quickstart.common.base.BaseService;
import com.winsalty.quickstart.common.constant.CommonStatusConstants;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.constant.SecurityConstants;
import com.winsalty.quickstart.common.constant.SystemConstants;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.common.util.IpUtils;
import com.winsalty.quickstart.permission.mapper.PermissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 认证服务实现。
 * 负责账号校验、令牌签发、Redis 会话维护、注册初始化和个人资料查询。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Service
public class AuthServiceImpl extends BaseService implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final long DEFAULT_DEPARTMENT_ID = 2L;
    private static final long DEFAULT_VIEWER_ROLE_ID = 2L;

    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;
    private final RegisterVerificationService registerVerificationService;

    public AuthServiceImpl(UserMapper userMapper,
                           PermissionMapper permissionMapper,
                           JwtTokenProvider jwtTokenProvider,
                           BCryptPasswordEncoder passwordEncoder,
                           AuthSessionService authSessionService,
                           RegisterVerificationService registerVerificationService) {
        this.userMapper = userMapper;
        this.permissionMapper = permissionMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.authSessionService = authSessionService;
        this.registerVerificationService = registerVerificationService;
    }

    /**
     * 登录主流程：校验账号密码 -> 校验账号状态 -> 查询角色 -> 签发双 Token -> 写入 Redis 会话。
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        UserEntity user = userMapper.findByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_BAD_CREDENTIALS);
        }
        if (!CommonStatusConstants.ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_UNAVAILABLE);
        }
        String roleCode = permissionMapper.findRoleCodeByUserId(user.getId());
        if (!StringUtils.hasText(roleCode)) {
            throw new BusinessException(ErrorCode.ROLE_NOT_ASSIGNED);
        }
        String sessionId = jwtTokenProvider.generateSessionId();
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), roleCode, sessionId);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getUsername(), roleCode, sessionId);
        authSessionService.createSession(sessionId, refreshToken, jwtTokenProvider.getRefreshExpireSeconds());
        log.info("user login success, username={}, roleCode={}, sessionId={}", user.getUsername(), roleCode, sessionId);
        LoginResponse response = new LoginResponse(accessToken, accessToken, refreshToken,
                jwtTokenProvider.getAccessExpireSeconds(), jwtTokenProvider.getRefreshExpireSeconds(), SecurityConstants.TOKEN_PREFIX_BEARER);
        response.setRoleCode(roleCode);
        response.setRoleName(permissionMapper.findRoleNameByUserId(user.getId()));
        return response;
    }

    /**
     * Refresh token 轮换：旧 refresh token 必须与 Redis 中当前 session 记录一致。
     * 轮换后旧 refresh token 立即失效，可降低泄露后的可用窗口。
     */
    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        TokenPayload payload = jwtTokenProvider.parseToken(request.getRefreshToken());
        if (!SecurityConstants.TOKEN_TYPE_REFRESH.equals(payload.getTokenType())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        if (!authSessionService.exists(payload.getSessionId())
                || !authSessionService.matchesRefreshToken(payload.getSessionId(), request.getRefreshToken())) {
            throw new BusinessException(ErrorCode.SESSION_INVALID);
        }
        String accessToken = jwtTokenProvider.createAccessToken(payload.getUserId(), payload.getUsername(), payload.getRoleCode(), payload.getSessionId());
        String refreshToken = jwtTokenProvider.createRefreshToken(payload.getUserId(), payload.getUsername(), payload.getRoleCode(), payload.getSessionId());
        authSessionService.refreshSession(payload.getSessionId(), refreshToken, jwtTokenProvider.getRefreshExpireSeconds());
        RefreshTokenResponse response = new RefreshTokenResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtTokenProvider.getAccessExpireSeconds());
        response.setRefreshExpiresIn(jwtTokenProvider.getRefreshExpireSeconds());
        response.setTokenType(SecurityConstants.TOKEN_PREFIX_BEARER);
        log.info("token refreshed, username={}, roleCode={}, sessionId={}", payload.getUsername(), payload.getRoleCode(), payload.getSessionId());
        return response;
    }

    /**
     * 删除当前会话的 Redis key，使该 refresh token 后续无法继续换取新令牌。
     */
    @Override
    public void logout(Long userId, String sessionId) {
        authSessionService.deleteSession(sessionId);
        log.info("user logout success, userId={}, sessionId={}", userId, sessionId);
    }

    /**
     * 注册用户默认分配 viewer 角色和运营中心部门，保证注册后可直接按只读权限登录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        registerVerificationService.verifyCode(request.getEmail(), request.getVerifyCode());
        UserEntity existedUser = userMapper.findByUsername(request.getUsername());
        if (existedUser != null) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        UserEntity user = new UserEntity();
        user.setRecordCode("U" + System.currentTimeMillis());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setNickname(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(CommonStatusConstants.ACTIVE);
        user.setOwner(SystemConstants.REGISTER_OWNER);
        user.setDescription(SystemConstants.REGISTER_DESCRIPTION);
        user.setDepartmentId(DEFAULT_DEPARTMENT_ID);
        user.setDeleted(0);
        userMapper.insert(user);
        userMapper.insertUserRole(user.getId(), DEFAULT_VIEWER_ROLE_ID);
        log.info("user register success, username={}, roleCode={}", user.getUsername(), SystemConstants.VIEWER_ROLE_CODE);
    }

    /**
     * 发送注册验证码并写入 Redis。验证码只通过邮件送达，不再返回给前端。
     */
    @Override
    public void sendRegisterVerifyCode(String email) {
        registerVerificationService.sendCode(email);
    }

    /**
     * 查询当前用户基础资料和角色信息，供前端刷新页面后恢复用户信息使用。
     */
    @Override
    public ProfileResponse getProfile(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() != null && user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
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
