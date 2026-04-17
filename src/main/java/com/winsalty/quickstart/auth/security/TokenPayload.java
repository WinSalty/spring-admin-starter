package com.winsalty.quickstart.auth.security;

/**
 * 令牌载荷对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class TokenPayload {

    private final Long userId;
    private final String username;
    private final String roleCode;
    private final String sessionId;
    private final String tokenType;

    public TokenPayload(Long userId, String username, String roleCode, String sessionId, String tokenType) {
        this.userId = userId;
        this.username = username;
        this.roleCode = roleCode;
        this.sessionId = sessionId;
        this.tokenType = tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getTokenType() {
        return tokenType;
    }
}
