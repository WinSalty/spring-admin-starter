package com.winsalty.quickstart.auth.service;

/**
 * 认证会话服务接口。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public interface AuthSessionService {

    void createSession(Long userId, String deviceType, String sessionId, String refreshToken, long timeoutSeconds);

    boolean exists(String sessionId);

    boolean matchesRefreshToken(String sessionId, String refreshToken);

    void refreshSession(String sessionId, String refreshToken, long timeoutSeconds);

    void deleteSession(String sessionId);
}
