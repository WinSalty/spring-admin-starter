package com.winsalty.quickstart.auth.security;

import lombok.Getter;

/**
 * 认证上下文对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Getter
public class AuthUser {

    private final Long userId;
    private final String username;
    private final String roleCode;
    private final String sessionId;

    public AuthUser(Long userId, String username, String roleCode, String sessionId) {
        this.userId = userId;
        this.username = username;
        this.roleCode = roleCode;
        this.sessionId = sessionId;
    }
}
