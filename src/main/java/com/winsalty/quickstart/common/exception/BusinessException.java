package com.winsalty.quickstart.common.exception;

import com.winsalty.quickstart.common.constant.ErrorCode;

/**
 * 业务异常。
 * 用于可预期的业务失败，例如参数不合法、资源不存在、权限不足。
 * 全局异常处理器会按业务码原样返回给前端。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
