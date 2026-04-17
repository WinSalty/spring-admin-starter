package com.winsalty.quickstart.auth.security;

/**
 * 当前线程认证上下文。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public final class AuthContext {

    private static final ThreadLocal<AuthUser> CURRENT_USER = new ThreadLocal<AuthUser>();

    private AuthContext() {
    }

    public static void set(AuthUser authUser) {
        CURRENT_USER.set(authUser);
    }

    public static AuthUser get() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
