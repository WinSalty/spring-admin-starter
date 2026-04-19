package com.winsalty.quickstart.common.base;

import com.winsalty.quickstart.auth.security.AuthContext;
import com.winsalty.quickstart.auth.security.AuthUser;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;

/**
 * 控制器基础能力。
 * 控制器层只暴露与请求上下文相关的便捷方法，具体业务规则仍放在 service。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
public abstract class BaseController {

    /**
     * 获取当前登录用户；若过滤器未注入登录态，则按未认证处理。
     */
    protected AuthUser requireCurrentUser() {
        AuthUser authUser = AuthContext.get();
        if (authUser == null) {
            // 理论上受保护接口会先被 Spring Security 拦截；这里保留兜底，避免直接调用 Controller 方法时 NPE。
            throw new BusinessException(ErrorCode.AUTH_REQUIRED);
        }
        return authUser;
    }

    protected Long currentUserId() {
        return requireCurrentUser().getUserId();
    }

    protected String currentUsername() {
        return requireCurrentUser().getUsername();
    }
}
