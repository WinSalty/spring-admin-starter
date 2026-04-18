package com.winsalty.quickstart.auth.security;

/**
 * 当前线程认证上下文。
 * 作为业务代码读取当前登录用户的轻量入口，数据由 JwtAuthenticationFilter 写入。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public final class AuthContext {

    private static final ThreadLocal<AuthUser> CURRENT_USER = new ThreadLocal<AuthUser>();

    private AuthContext() {
    }

    /**
     * 设置当前请求用户，仅应由认证过滤器调用。
     */
    public static void set(AuthUser authUser) {
        CURRENT_USER.set(authUser);
    }

    public static AuthUser get() {
        return CURRENT_USER.get();
    }

    /**
     * 清理 ThreadLocal，必须在请求结束时执行。
     */
    public static void clear() {
        CURRENT_USER.remove();
    }
}
