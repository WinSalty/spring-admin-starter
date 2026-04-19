package com.winsalty.quickstart.common.base;

import com.winsalty.quickstart.auth.security.AuthContext;
import com.winsalty.quickstart.auth.security.AuthUser;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.constant.SystemConstants;
import com.winsalty.quickstart.common.exception.BusinessException;

/**
 * 服务基础能力。
 * 统一封装当前登录用户读取逻辑，避免各业务服务直接依赖 ThreadLocal 细节。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
public abstract class BaseService {

    /**
     * 获取当前登录用户；仅适用于受 JWT 保护的业务入口。
     */
    protected AuthUser requireCurrentUser() {
        AuthUser authUser = AuthContext.get();
        if (authUser == null) {
            // Service 可能被定时任务或测试直接调用，缺少认证上下文时用业务异常表达调用前置条件不满足。
            throw new BusinessException(ErrorCode.AUTH_REQUIRED);
        }
        return authUser;
    }

    /**
     * 当前登录用户 ID，通常用于写入 created_by/updated_by 或权限过滤。
     */
    protected Long currentUserId() {
        return requireCurrentUser().getUserId();
    }

    /**
     * 当前操作人名称。后台任务或异常兜底场景没有登录态时返回 system。
     */
    protected String currentUsername() {
        AuthUser authUser = AuthContext.get();
        // 审计日志、异常日志等非用户触发场景没有登录态，统一归属到 system 操作人。
        return authUser == null ? SystemConstants.SYSTEM_OPERATOR : authUser.getUsername();
    }
}
