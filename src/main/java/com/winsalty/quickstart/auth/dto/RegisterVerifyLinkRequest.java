package com.winsalty.quickstart.auth.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * 注册邮箱验证链接请求对象。
 * 承载邮件链接中的邮箱和一次性 token，由前端回调后端完成验证。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class RegisterVerifyLinkRequest {

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "邮箱验证 token 不能为空")
    private String token;
}
