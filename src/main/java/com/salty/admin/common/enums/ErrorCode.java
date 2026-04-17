package com.salty.admin.common.enums;

public enum ErrorCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(40001, "参数校验失败"),
    UNAUTHORIZED(40101, "登录已过期"),
    FORBIDDEN(40301, "无操作权限"),
    BUSINESS_CONFLICT(40901, "业务冲突"),
    TOO_MANY_REQUESTS(42901, "请求过于频繁"),
    SYSTEM_ERROR(50000, "系统内部异常");

    private final int code;

    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
