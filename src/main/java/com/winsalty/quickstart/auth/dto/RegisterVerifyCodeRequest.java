package com.winsalty.quickstart.auth.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * 注册验证码发送请求对象。
 * 使用请求体承载邮箱，避免 GET query 泄露邮箱并明确接口具备发送副作用。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class RegisterVerifyCodeRequest {

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;
}
