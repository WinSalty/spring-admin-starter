package com.winsalty.quickstart.auth.service.impl;

import com.winsalty.quickstart.auth.service.AuthSessionService;
import com.winsalty.quickstart.common.constant.SecurityConstants;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 认证会话服务实现。
 * 使用 Redis 记录 session、用户设备索引和会话归属，用于令牌轮换、登出失效和同设备单点登录。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Service
public class AuthSessionServiceImpl implements AuthSessionService {

    private static final String SESSION_KEY_PREFIX = "sa:auth:session:";
    private static final String SESSION_OWNER_KEY_PREFIX = "sa:auth:session-owner:";
    private static final String USER_DEVICE_KEY_PREFIX = "sa:auth:user-device:";
    private static final String OWNER_SEPARATOR = ":";

    private final RedisCacheService redisCacheService;

    public AuthSessionServiceImpl(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    /**
     * 登录时创建会话，并踢掉同一用户同一设备类型的旧会话。
     * 旧 access token 会因为 session key 被删除而在过滤器中立即失效。
     */
    @Override
    public void createSession(Long userId, String deviceType, String sessionId, String refreshToken, long timeoutSeconds) {
        String normalizedDeviceType = normalizeDeviceType(deviceType);
        String userDeviceKey = userDeviceKey(userId, normalizedDeviceType);
        Object oldSessionId = redisCacheService.get(userDeviceKey);
        if (oldSessionId instanceof String && StringUtils.hasText((String) oldSessionId)) {
            // 同一用户同一设备类型只保留一个会话，登录成功后立即踢掉旧会话。
            deleteSession((String) oldSessionId);
        }
        // session key 存 refresh token，owner key 存用户设备归属，user-device key 存最新 sessionId。
        redisCacheService.set(SESSION_KEY_PREFIX + sessionId, refreshToken, timeoutSeconds);
        redisCacheService.set(SESSION_OWNER_KEY_PREFIX + sessionId, ownerValue(userId, normalizedDeviceType), timeoutSeconds);
        redisCacheService.set(userDeviceKey, sessionId, timeoutSeconds);
    }

    @Override
    public boolean exists(String sessionId) {
        // refresh token 解析成功不代表会话仍有效，Redis key 可被登出或过期清理。
        return redisCacheService.get(SESSION_KEY_PREFIX + sessionId) != null;
    }

    /**
     * 校验请求携带的 refresh token 是否仍是该 session 当前有效版本。
     */
    @Override
    public boolean matchesRefreshToken(String sessionId, String refreshToken) {
        Object cached = redisCacheService.get(SESSION_KEY_PREFIX + sessionId);
        // 只接受 Redis 中当前保存的 refresh token，旧 token 会因为轮换覆盖而不再匹配。
        return cached instanceof String && StringUtils.hasText(refreshToken) && refreshToken.equals(cached);
    }

    /**
     * 刷新令牌后覆盖 Redis 中的 refresh token，实现单 session 内的令牌轮换。
     */
    @Override
    public void refreshSession(String sessionId, String refreshToken, long timeoutSeconds) {
        redisCacheService.set(SESSION_KEY_PREFIX + sessionId, refreshToken, timeoutSeconds);
        Object owner = redisCacheService.get(SESSION_OWNER_KEY_PREFIX + sessionId);
        if (owner instanceof String && StringUtils.hasText((String) owner)) {
            // refresh token 轮换时同步续期 owner 和 user-device 索引，避免会话主体 key 还在但索引过期。
            redisCacheService.set(SESSION_OWNER_KEY_PREFIX + sessionId, owner, timeoutSeconds);
            redisCacheService.set(userDeviceKey((String) owner), sessionId, timeoutSeconds);
        }
    }

    @Override
    public void deleteSession(String sessionId) {
        Object owner = redisCacheService.get(SESSION_OWNER_KEY_PREFIX + sessionId);
        // 删除 session 后 refresh token 无法续期；过滤器也会拒绝同一 session 的 access token。
        redisCacheService.delete(SESSION_KEY_PREFIX + sessionId);
        redisCacheService.delete(SESSION_OWNER_KEY_PREFIX + sessionId);
        if (owner instanceof String && StringUtils.hasText((String) owner)) {
            String userDeviceKey = userDeviceKey((String) owner);
            Object currentSessionId = redisCacheService.get(userDeviceKey);
            if (sessionId.equals(currentSessionId)) {
                // 只删除仍指向当前 session 的索引，避免并发新登录后误删新会话索引。
                redisCacheService.delete(userDeviceKey);
            }
        }
    }

    private String normalizeDeviceType(String deviceType) {
        return StringUtils.hasText(deviceType) ? deviceType.trim().toUpperCase() : SecurityConstants.DEFAULT_DEVICE_TYPE;
    }

    private String ownerValue(Long userId, String deviceType) {
        return userId + OWNER_SEPARATOR + normalizeDeviceType(deviceType);
    }

    private String userDeviceKey(Long userId, String deviceType) {
        return USER_DEVICE_KEY_PREFIX + ownerValue(userId, deviceType);
    }

    private String userDeviceKey(String owner) {
        return USER_DEVICE_KEY_PREFIX + owner;
    }
}
