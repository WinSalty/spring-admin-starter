package com.winsalty.quickstart.infra.verification;

import lombok.Data;

/**
 * 邮箱验证码校验请求。
 * 由业务模块传入场景编码、邮箱和用户提交的验证码。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class EmailVerificationCodeVerifyRequest {

    /**
     * 业务场景编码，必须和发送验证码时一致。
     */
    private String scene;

    /**
     * 收件邮箱。
     */
    private String email;

    /**
     * 用户提交的验证码。
     */
    private String code;
}
