package com.winsalty.quickstart.auth.service.impl;

import com.winsalty.quickstart.auth.dto.LoginRequest;
import com.winsalty.quickstart.auth.dto.NotificationSettingsRequest;
import com.winsalty.quickstart.auth.dto.PasswordUpdateRequest;
import com.winsalty.quickstart.auth.dto.ProfileUpdateRequest;
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
    private static final String REGISTER_SCENE_SUBMIT = "submit-register";
    private static final String REGISTER_SCENE_RESEND = "resend-register";
    private static final String REGISTER_SCENE_ACTIVATE = "activate-register";
    private static final String REGISTER_AVAILABILITY_USERNAME_EXISTS = "username_exists";
    private static final String REGISTER_AVAILABILITY_EMAIL_EXISTS = "email_exists";
    private static final String REGISTER_RECORD_CODE_PREFIX = "U";
    private static final String DEFAULT_COUNTRY = "中国";
    private static final String DEFAULT_PHONE_PREFIX = "86";
    private static final int ENABLED_FLAG = 1;
    private static final int DISABLED_FLAG = 0;
    private static final int NOT_DELETED = 0;
    private static final String MASKED_VALUE = "***";
    private static final String SINGLE_CHAR_MASK = "*";
    private static final char EMAIL_SEPARATOR = '@';
    private static final int SINGLE_CHARACTER_LENGTH = 1;

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
        String account = normalizeAccount(request.getUsername());
        UserEntity user = userMapper.findByUsernameOrEmail(account);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 用户不存在和密码错误使用同一提示，防止通过接口枚举有效用户名。
            throw new BusinessException(ErrorCode.LOGIN_BAD_CREDENTIALS);
        }
        if (CommonStatusConstants.PENDING.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_UNAVAILABLE, "账号尚未激活，请先点击注册邮件中的激活链接");
        }
        if (!CommonStatusConstants.ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_UNAVAILABLE);
        }
        String roleCode = permissionMapper.findRoleCodeByUserId(user.getId());
        if (!StringUtils.hasText(roleCode)) {
            throw new BusinessException(ErrorCode.ROLE_NOT_ASSIGNED);
        }
        // sessionId 同时写入 access/refresh token，后续 refresh 时用它定位 Redis 中的当前会话。
        String deviceType = normalizeDeviceType(request.getDeviceType());
        String sessionId = jwtTokenProvider.generateSessionId();
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), roleCode, sessionId);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getUsername(), roleCode, sessionId);
        // 同一账号同一设备类型只保留最新 session，旧 access token 会因 session 失效被过滤器拒绝。
        authSessionService.createSession(user.getId(), deviceType, sessionId, refreshToken, jwtTokenProvider.getRefreshExpireSeconds());
        log.info("user login success, username={}, roleCode={}, deviceType={}, sessionId={}", user.getUsername(), roleCode, deviceType, sessionId);
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
            // access token 不能用于换取新 token，避免权限边界混用。
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        if (!authSessionService.exists(payload.getSessionId())
                || !authSessionService.matchesRefreshToken(payload.getSessionId(), request.getRefreshToken())) {
            // session 不存在通常代表已登出；refresh token 不匹配代表已被轮换或疑似重放。
            throw new BusinessException(ErrorCode.SESSION_INVALID);
        }
        String accessToken = jwtTokenProvider.createAccessToken(payload.getUserId(), payload.getUsername(), payload.getRoleCode(), payload.getSessionId());
        String refreshToken = jwtTokenProvider.createRefreshToken(payload.getUserId(), payload.getUsername(), payload.getRoleCode(), payload.getSessionId());
        // 每次刷新都覆盖 Redis 中的 refresh token，旧 refresh token 随即失效。
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
     * 注册用户默认分配 viewer 角色和运营中心部门，账号激活前使用 pending 状态阻止登录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request, String verifyLinkBaseUrl) {
        String username = normalizeAccount(request.getUsername());
        String email = normalizeEmail(request.getEmail());
        log.info("user register request received, username={}, email={}", username, maskEmail(email));
        try {
            UserEntity user = savePendingRegisterUser(username, email, request.getPassword());
            registerVerificationService.sendVerificationLink(email, verifyLinkBaseUrl);
            log.info("user register activation mail sent, username={}, userId={}, status={}",
                    user.getUsername(), user.getId(), user.getStatus());
        } catch (BusinessException exception) {
            logRegisterFailure(REGISTER_SCENE_SUBMIT, username, email, exception);
            throw exception;
        }
    }

    /**
     * 为待激活注册账号重新发送验证邮件。只允许 pending 账号重发，避免向非待激活账号发送邮件。
     */
    @Override
    public void resendRegisterVerifyMail(String email, String verifyLinkBaseUrl) {
        String normalizedEmail = normalizeEmail(email);
        try {
            UserEntity pendingUser = userMapper.findByEmail(normalizedEmail);
            if (pendingUser == null || !CommonStatusConstants.PENDING.equals(pendingUser.getStatus())) {
                throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_INVALID, "待激活账号不存在或已激活");
            }
            registerVerificationService.sendVerificationLink(normalizedEmail, verifyLinkBaseUrl);
            log.info("register activation mail resent, username={}, userId={}, email={}",
                    pendingUser.getUsername(), pendingUser.getId(), maskEmail(normalizedEmail));
        } catch (BusinessException exception) {
            logRegisterFailure(REGISTER_SCENE_RESEND, "", normalizedEmail, exception);
            throw exception;
        }
    }

    /**
     * 校验注册账号激活链接。链接验证成功后把待激活账号切换为 active。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyRegisterEmail(String email, String token) {
        String normalizedEmail = normalizeEmail(email);
        try {
            UserEntity pendingUser = userMapper.findByEmail(normalizedEmail);
            if (pendingUser == null || !CommonStatusConstants.PENDING.equals(pendingUser.getStatus())) {
                throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_INVALID, "待激活账号不存在或已激活");
            }
            registerVerificationService.verifyLink(normalizedEmail, token);
            int updated = userMapper.activatePendingByEmail(normalizedEmail);
            if (updated <= 0) {
                throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_INVALID, "待激活账号不存在或已激活");
            }
            log.info("register account activated, email={}", maskEmail(normalizedEmail));
        } catch (BusinessException exception) {
            logRegisterFailure(REGISTER_SCENE_ACTIVATE, "", normalizedEmail, exception);
            throw exception;
        }
    }

    /**
     * 查询当前用户基础资料和角色信息，供前端刷新页面后恢复用户信息使用。
     */
    @Override
    public ProfileResponse getProfile(Long userId) {
        UserEntity user = userMapper.findActiveById(userId);
        if (user == null || user.getDeleted() != null && user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return buildProfileResponse(user);
    }

    @Override
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        UserEntity user = userMapper.findActiveById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 个人资料允许部分字段为空，入库前统一 trim，避免前端传入空格造成展示异常。
        user.setEmail(trimToNull(request.getEmail()));
        user.setNickname(trimToNull(request.getNickname()));
        user.setDescription(trimToDefault(request.getDescription(), ""));
        // 头像地址只接受后端文件接口生成的稳定地址，不允许浏览器 blob 临时地址落库。
        user.setAvatarUrl(normalizeAvatarUrl(request.getAvatarUrl()));
        user.setCountry(trimToDefault(request.getCountry(), "中国"));
        user.setProvince(trimToNull(request.getProvince()));
        user.setCity(trimToNull(request.getCity()));
        user.setStreetAddress(trimToNull(request.getStreetAddress()));
        user.setPhonePrefix(trimToNull(request.getPhonePrefix()));
        user.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        userMapper.updateProfile(user);
        log.info("user profile updated, userId={}, username={}", userId, user.getUsername());
        return getProfile(userId);
    }

    @Override
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        UserEntity user = userMapper.findActiveById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            // 修改密码必须校验当前密码，避免登录态被盗后直接静默改密。
            throw new BusinessException(ErrorCode.LOGIN_BAD_CREDENTIALS, "当前密码不正确");
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()));
        log.info("user password updated, userId={}, username={}", userId, user.getUsername());
    }

    @Override
    public ProfileResponse updateNotificationSettings(Long userId, NotificationSettingsRequest request) {
        UserEntity user = userMapper.findActiveById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setNotifyAccount(Boolean.TRUE.equals(request.getNotifyAccount()) ? 1 : 0);
        user.setNotifySystem(Boolean.TRUE.equals(request.getNotifySystem()) ? 1 : 0);
        user.setNotifyTodo(Boolean.TRUE.equals(request.getNotifyTodo()) ? 1 : 0);
        userMapper.updateNotificationSettings(user);
        log.info("user notification settings updated, userId={}, account={}, system={}, todo={}",
                userId, user.getNotifyAccount(), user.getNotifySystem(), user.getNotifyTodo());
        return getProfile(userId);
    }

    private ProfileResponse buildProfileResponse(UserEntity user) {
        ProfileResponse response = new ProfileResponse();
        // ProfileResponse 聚合用户基础资料和角色信息，供前端刷新后恢复登录上下文。
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setDescription(user.getDescription());
        response.setAvatarUrl(resolveReadableAvatarUrl(user.getAvatarUrl()));
        response.setCountry(StringUtils.hasText(user.getCountry()) ? user.getCountry() : "中国");
        response.setProvince(user.getProvince());
        response.setCity(user.getCity());
        response.setStreetAddress(user.getStreetAddress());
        response.setPhonePrefix(user.getPhonePrefix());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setNotifyAccount(toBoolean(user.getNotifyAccount(), true));
        response.setNotifySystem(toBoolean(user.getNotifySystem(), true));
        response.setNotifyTodo(toBoolean(user.getNotifyTodo(), false));
        response.setRoleCode(permissionMapper.findRoleCodeByUserId(user.getId()));
        response.setRoleName(permissionMapper.findRoleNameByUserId(user.getId()));
        return response;
    }

    private Boolean toBoolean(Integer value, boolean defaultValue) {
        if (value == null) {
            // 历史数据可能没有通知开关字段，按产品默认值返回，避免前端出现 null。
            return defaultValue;
        }
        return value == 1;
    }

    private String normalizeDeviceType(String deviceType) {
        return StringUtils.hasText(deviceType) ? deviceType.trim().toUpperCase() : SecurityConstants.DEFAULT_DEVICE_TYPE;
    }

    private String normalizeAccount(String account) {
        if (!StringUtils.hasText(account)) {
            return "";
        }
        return account.trim();
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "";
        }
        return email.trim().toLowerCase();
    }

    private UserEntity savePendingRegisterUser(String username, String email, String password) {
        UserEntity existedUser = userMapper.findByUsername(username);
        UserEntity existedEmailUser = userMapper.findByEmail(email);
        // pending 账号允许原用户名和原邮箱重新提交注册，用于用户未收到激活邮件时刷新密码和链接。
        UserEntity pendingUser = resolveReusablePendingRegisterUser(username, email, existedUser, existedEmailUser);
        String encodedPassword = passwordEncoder.encode(password);
        if (pendingUser != null) {
            userMapper.updatePendingRegistration(pendingUser.getId(), encodedPassword);
            pendingUser.setPassword(encodedPassword);
            log.info("pending register account refreshed, username={}, userId={}", username, pendingUser.getId());
            return pendingUser;
        }
        UserEntity user = new UserEntity();
        user.setRecordCode(REGISTER_RECORD_CODE_PREFIX + System.currentTimeMillis());
        user.setUsername(username);
        user.setEmail(email);
        // 注册用户默认昵称使用用户名，用户激活后可在个人中心修改。
        user.setNickname(username);
        user.setCountry(DEFAULT_COUNTRY);
        user.setPhonePrefix(DEFAULT_PHONE_PREFIX);
        user.setNotifyAccount(ENABLED_FLAG);
        user.setNotifySystem(ENABLED_FLAG);
        user.setNotifyTodo(DISABLED_FLAG);
        user.setPassword(encodedPassword);
        user.setStatus(CommonStatusConstants.PENDING);
        user.setOwner(SystemConstants.REGISTER_OWNER);
        user.setDescription(SystemConstants.REGISTER_DESCRIPTION);
        // 当前 starter 使用初始化 SQL 中的运营中心部门和 viewer 角色作为注册用户默认归属。
        user.setDepartmentId(DEFAULT_DEPARTMENT_ID);
        user.setDeleted(NOT_DELETED);
        userMapper.insert(user);
        userMapper.insertUserRole(user.getId(), DEFAULT_VIEWER_ROLE_ID);
        log.info("pending register account created, username={}, userId={}, roleCode={}",
                user.getUsername(), user.getId(), SystemConstants.VIEWER_ROLE_CODE);
        return user;
    }

    private UserEntity resolveReusablePendingRegisterUser(String username, String email,
                                                          UserEntity existedUser, UserEntity existedEmailUser) {
        if (existedUser == null && existedEmailUser == null) {
            return null;
        }
        if (existedUser != null && !email.equals(normalizeEmail(existedUser.getEmail()))) {
            // 用户名已被其他邮箱占用时直接拒绝，避免用 pending 复用路径覆盖他人账号。
            rejectRegisterAvailability(username, email, REGISTER_AVAILABILITY_USERNAME_EXISTS,
                    ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (existedEmailUser != null && !username.equals(normalizeAccount(existedEmailUser.getUsername()))) {
            // 邮箱已绑定其他用户名时直接拒绝，避免邮箱所有权被后续注册流程抢占。
            rejectRegisterAvailability(username, email, REGISTER_AVAILABILITY_EMAIL_EXISTS,
                    ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        UserEntity existed = existedUser != null ? existedUser : existedEmailUser;
        if (!CommonStatusConstants.PENDING.equals(existed.getStatus())) {
            // active/disabled 账号不能走注册刷新逻辑，必须提示账号或邮箱已存在。
            ErrorCode errorCode = existedUser != null ? ErrorCode.USERNAME_ALREADY_EXISTS : ErrorCode.EMAIL_ALREADY_EXISTS;
            String reason = existedUser != null ? REGISTER_AVAILABILITY_USERNAME_EXISTS : REGISTER_AVAILABILITY_EMAIL_EXISTS;
            rejectRegisterAvailability(username, email, reason, errorCode);
        }
        return existed;
    }

    private void rejectRegisterAvailability(String username, String email, String reason, ErrorCode errorCode) {
        log.error("register availability check failed, scene={}, username={}, email={}, reason={}",
                REGISTER_SCENE_SUBMIT, username, maskEmail(email), reason);
        throw new BusinessException(errorCode);
    }

    private void logRegisterFailure(String scene, String username, String email, BusinessException exception) {
        log.error("register flow failed, scene={}, username={}, email={}, code={}, message={}",
                scene, username, maskEmail(email), exception.getCode(), exception.getMessage());
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return MASKED_VALUE;
        }
        int atIndex = email.indexOf(EMAIL_SEPARATOR);
        if (atIndex <= 0) {
            return MASKED_VALUE;
        }
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        if (localPart.length() == SINGLE_CHARACTER_LENGTH) {
            return SINGLE_CHAR_MASK + domainPart;
        }
        return localPart.charAt(0) + MASKED_VALUE
                + localPart.charAt(localPart.length() - SINGLE_CHARACTER_LENGTH) + domainPart;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String trimToDefault(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * 头像必须是后端文件接口或对象存储返回的持久 URL，禁止保存浏览器本地 blob 临时地址。
     *
     * @param value 原始头像地址
     * @return 可持久化头像地址
     * @author sunshengxian
     * @date 2026-04-23
     */
    private String normalizeAvatarUrl(String value) {
        String avatarUrl = trimToNull(value);
        if (avatarUrl == null) {
            return null;
        }
        if (isPersistentAvatarUrl(avatarUrl)) {
            return avatarUrl;
        }
        throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "头像地址不合法");
    }

    /**
     * 读取用户信息时过滤历史脏头像地址，避免临时 blob 地址影响前端展示。
     *
     * @param value 数据库存储头像地址
     * @return 可读头像地址
     * @author sunshengxian
     * @date 2026-04-23
     */
    private String resolveReadableAvatarUrl(String value) {
        String avatarUrl = trimToNull(value);
        if (avatarUrl == null) {
            return null;
        }
        if (isPersistentAvatarUrl(avatarUrl)) {
            return avatarUrl;
        }
        return null;
    }

    /**
     * 判断头像地址是否为可长期读取的持久地址。
     *
     * @param avatarUrl 头像地址
     * @return 是否为持久地址
     * @author sunshengxian
     * @date 2026-04-23
     */
    private boolean isPersistentAvatarUrl(String avatarUrl) {
        // 当前只允许后端头像代理地址；若未来支持 CDN 永久地址，需要在这里显式扩展白名单。
        return avatarUrl.startsWith("/api/file/avatar/");
    }
}
