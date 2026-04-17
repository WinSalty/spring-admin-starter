package com.salty.admin.common.base;

import com.salty.admin.common.security.SecurityUtils;

public abstract class BaseController {

    protected Long currentUserId() {
        return SecurityUtils.getUserId();
    }

    protected String currentUsername() {
        return SecurityUtils.getUsername();
    }
}
