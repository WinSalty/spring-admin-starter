package com.winsalty.quickstart.auth.security;

import lombok.Getter;

/**
 * 令牌载荷对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Getter
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
}
