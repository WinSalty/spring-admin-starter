package com.salty.admin.common.security;

public final class LoginUserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<LoginUser>();

    private LoginUserContext() {
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static void clear() {
        HOLDER.remove();
    }
}
