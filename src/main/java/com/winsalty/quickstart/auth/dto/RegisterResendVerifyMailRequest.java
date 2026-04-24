package com.winsalty.quickstart.auth.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * 注册验证邮件重发请求对象。
 * 承载待激活账号邮箱，用于重新生成一次性激活链接并发送验证邮件。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class RegisterResendVerifyMailRequest {

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;
}
