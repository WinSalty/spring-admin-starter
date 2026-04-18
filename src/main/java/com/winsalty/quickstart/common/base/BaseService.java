package com.winsalty.quickstart.common.base;

import com.winsalty.quickstart.auth.security.AuthContext;
import com.winsalty.quickstart.auth.security.AuthUser;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.constant.SystemConstants;
import com.winsalty.quickstart.common.exception.BusinessException;

/**
 * 服务基础能力。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
public abstract class BaseService {

    protected AuthUser requireCurrentUser() {
        AuthUser authUser = AuthContext.get();
        if (authUser == null) {
            throw new BusinessException(ErrorCode.AUTH_REQUIRED);
        }
        return authUser;
    }

    protected Long currentUserId() {
        return requireCurrentUser().getUserId();
    }

    protected String currentUsername() {
        AuthUser authUser = AuthContext.get();
        return authUser == null ? SystemConstants.SYSTEM_OPERATOR : authUser.getUsername();
    }
}
