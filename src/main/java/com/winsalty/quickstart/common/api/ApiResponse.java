package com.winsalty.quickstart.common.api;

import lombok.Data;

/**
 * 统一响应对象。
 * 所有 JSON 接口都应使用该结构，前端只需要判断 code 是否为 0。
 * HTTP 状态码主要表达传输层结果，业务失败通过 code/message 表达。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 默认成功响应，message 固定为 success。
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(0, "success", data);
    }

    /**
     * 带业务提示的成功响应，适用于保存、上传、状态切换等用户可感知动作。
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<T>(0, message, data);
    }

    /**
     * 统一失败响应，data 始终为空，避免前端误用失败场景下的脏数据。
     */
    public static <T> ApiResponse<T> failure(int code, String message) {
        return new ApiResponse<T>(code, message, null);
    }
}
