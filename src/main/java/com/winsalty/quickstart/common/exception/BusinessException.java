package com.winsalty.quickstart.common.exception;

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

    public int getCode() {
        return code;
    }
}
