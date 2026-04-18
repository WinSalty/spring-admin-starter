package com.winsalty.quickstart.auth.service.impl;

import com.winsalty.quickstart.auth.service.AuthSessionService;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 认证会话服务实现。
 * 使用 Redis 记录 sessionId -> refreshToken，用于 refresh token 轮换和登出失效。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Service
public class AuthSessionServiceImpl implements AuthSessionService {

    private static final String SESSION_KEY_PREFIX = "sa:auth:session:";

    private final RedisCacheService redisCacheService;

    public AuthSessionServiceImpl(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    /**
     * 登录时创建会话，过期时间与 refresh token 过期时间保持一致。
     */
    @Override
    public void createSession(String sessionId, String refreshToken, long timeoutSeconds) {
        redisCacheService.set(SESSION_KEY_PREFIX + sessionId, refreshToken, timeoutSeconds);
    }

    @Override
    public boolean exists(String sessionId) {
        return redisCacheService.get(SESSION_KEY_PREFIX + sessionId) != null;
    }

    /**
     * 校验请求携带的 refresh token 是否仍是该 session 当前有效版本。
     */
    @Override
    public boolean matchesRefreshToken(String sessionId, String refreshToken) {
        Object cached = redisCacheService.get(SESSION_KEY_PREFIX + sessionId);
        return cached instanceof String && StringUtils.hasText(refreshToken) && refreshToken.equals(cached);
    }

    /**
     * 刷新令牌后覆盖 Redis 中的 refresh token，实现单 session 内的令牌轮换。
     */
    @Override
    public void refreshSession(String sessionId, String refreshToken, long timeoutSeconds) {
        redisCacheService.set(SESSION_KEY_PREFIX + sessionId, refreshToken, timeoutSeconds);
    }

    @Override
    public void deleteSession(String sessionId) {
        redisCacheService.delete(SESSION_KEY_PREFIX + sessionId);
    }
}
