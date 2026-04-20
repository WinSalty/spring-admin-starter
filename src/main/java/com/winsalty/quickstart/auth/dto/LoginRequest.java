package com.winsalty.quickstart.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 登录请求对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 设备类型用于同账号同设备类型单点登录；为空时按 Web 端处理。
     * 常见取值：WEB、MOBILE、DESKTOP。服务端会统一转为大写。
     */
    @Size(max = 32, message = "设备类型长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "设备类型只能包含字母、数字、下划线或中划线")
    private String deviceType;
}
