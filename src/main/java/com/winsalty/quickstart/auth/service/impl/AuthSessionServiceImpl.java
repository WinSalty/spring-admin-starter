package com.winsalty.quickstart.auth.service.impl;

import com.winsalty.quickstart.auth.service.AuthSessionService;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 认证会话服务实现。
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

    @Override
    public void createSession(String sessionId, String refreshToken, long timeoutSeconds) {
        redisCacheService.set(SESSION_KEY_PREFIX + sessionId, refreshToken, timeoutSeconds);
    }

    @Override
    public boolean exists(String sessionId) {
        return redisCacheService.get(SESSION_KEY_PREFIX + sessionId) != null;
    }

    @Override
    public boolean matchesRefreshToken(String sessionId, String refreshToken) {
        Object cached = redisCacheService.get(SESSION_KEY_PREFIX + sessionId);
        return cached instanceof String && StringUtils.hasText(refreshToken) && refreshToken.equals(cached);
    }

    @Override
    public void refreshSession(String sessionId, String refreshToken, long timeoutSeconds) {
        redisCacheService.set(SESSION_KEY_PREFIX + sessionId, refreshToken, timeoutSeconds);
    }

    @Override
    public void deleteSession(String sessionId) {
        redisCacheService.delete(SESSION_KEY_PREFIX + sessionId);
    }
}
