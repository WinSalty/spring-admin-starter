package com.salty.admin.auth.service;

import com.salty.admin.common.enums.ErrorCode;
import com.salty.admin.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PasswordPolicyValidator {

    public void validate(String password, String confirmPassword) {
        if (!StringUtils.hasText(password) || password.length() < 8 || password.length() > 32) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码长度必须为 8-32 位");
        }
        if (StringUtils.hasText(confirmPassword) && !password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入密码不一致");
        }
        int categories = 0;
        if (password.matches(".*[a-z].*")) {
            categories++;
        }
        if (password.matches(".*[A-Z].*")) {
            categories++;
        }
        if (password.matches(".*[0-9].*")) {
            categories++;
        }
        if (password.matches(".*[^a-zA-Z0-9].*")) {
            categories++;
        }
        if (categories < 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码至少包含大小写字母、数字、特殊字符中的 3 类");
        }
    }
}
