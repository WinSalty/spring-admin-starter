package com.winsalty.quickstart.common.exception;

import com.winsalty.quickstart.common.constant.ErrorCode;

/**
 * 业务异常。
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
