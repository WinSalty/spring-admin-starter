package com.winsalty.quickstart.auth.service.impl;

import com.winsalty.quickstart.infra.cache.RedisCacheService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证会话服务测试。
 * 聚焦同设备单点登录和 session 反向索引清理，避免旧 token 被误保留或误删新会话。
 */
class AuthSessionServiceImplTest {

    private static final String SESSION_KEY_PREFIX = "sa:auth:session:";
    private static final String SESSION_OWNER_KEY_PREFIX = "sa:auth:session-owner:";
    private static final String USER_DEVICE_KEY_PREFIX = "sa:auth:user-device:";

    @Test
    void createSessionDeletesPreviousSessionForSameUserDevice() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        AuthSessionServiceImpl service = new AuthSessionServiceImpl(redisCacheService);
        String userDeviceKey = USER_DEVICE_KEY_PREFIX + "1:WEB";

        when(redisCacheService.get(userDeviceKey)).thenReturn("old-session", "old-session");
        when(redisCacheService.get(SESSION_OWNER_KEY_PREFIX + "old-session")).thenReturn("1:WEB");

        service.createSession(1L, "web", "new-session", "new-refresh-token", 600L);

        verify(redisCacheService).delete(SESSION_KEY_PREFIX + "old-session");
        verify(redisCacheService).delete(SESSION_OWNER_KEY_PREFIX + "old-session");
        verify(redisCacheService).delete(userDeviceKey);
        verify(redisCacheService).set(SESSION_KEY_PREFIX + "new-session", "new-refresh-token", 600L);
        verify(redisCacheService).set(SESSION_OWNER_KEY_PREFIX + "new-session", "1:WEB", 600L);
        verify(redisCacheService).set(userDeviceKey, "new-session", 600L);
    }

    @Test
    void deleteSessionKeepsUserDeviceIndexWhenItAlreadyPointsToNewSession() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        AuthSessionServiceImpl service = new AuthSessionServiceImpl(redisCacheService);
        String userDeviceKey = USER_DEVICE_KEY_PREFIX + "1:WEB";

        when(redisCacheService.get(SESSION_OWNER_KEY_PREFIX + "old-session")).thenReturn("1:WEB");
        when(redisCacheService.get(userDeviceKey)).thenReturn("new-session");

        service.deleteSession("old-session");

        verify(redisCacheService).delete(SESSION_KEY_PREFIX + "old-session");
        verify(redisCacheService).delete(SESSION_OWNER_KEY_PREFIX + "old-session");
        verify(redisCacheService, never()).delete(userDeviceKey);
    }
}
