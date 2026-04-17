package com.salty.admin.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser == null ? null : loginUser.getUserId();
    }

    public static LoginUser getLoginUser() {
        LoginUser contextUser = LoginUserContext.get();
        if (contextUser != null) {
            return contextUser;
        }
        Object principal = getPrincipal();
        return principal instanceof LoginUser ? (LoginUser) principal : null;
    }

    public static String getUsername() {
        LoginUser loginUser = getLoginUser();
        if (loginUser != null) {
            return loginUser.getUsername();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    public static boolean hasRole(String roleCode) {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null || roleCode == null) {
            return false;
        }
        return loginUser.getRoles().contains(roleCode);
    }

    public static boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || permission == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (permission.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private static Object getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getPrincipal();
    }
}
