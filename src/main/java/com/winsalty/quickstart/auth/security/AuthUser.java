package com.winsalty.quickstart.auth.security;

/**
 * 认证上下文对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class AuthUser {

    private final Long userId;
    private final String username;
    private final String roleCode;

    public AuthUser(Long userId, String username, String roleCode) {
        this.userId = userId;
        this.username = username;
        this.roleCode = roleCode;
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
}
