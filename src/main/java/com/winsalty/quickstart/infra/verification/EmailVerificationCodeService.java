package com.winsalty.quickstart.infra.verification;

/**
 * 通用邮箱验证码服务。
 * 为密码重置、换绑邮箱、敏感操作确认等业务提供场景隔离的验证码发送和校验能力。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
public interface EmailVerificationCodeService {

    /**
     * 发送邮箱验证码。
     */
    void sendCode(EmailVerificationCodeSendRequest request);

    /**
     * 校验验证码，成功后写入短期已验证状态，后续由业务提交动作一次性消费。
     */
    void verifyCode(EmailVerificationCodeVerifyRequest request);

    /**
     * 校验并立即消费验证码，适用于验证码提交和业务动作在同一次请求内完成的场景。
     */
    void consumeCode(EmailVerificationCodeVerifyRequest request);

    /**
     * 消费 verifyCode 写入的已验证状态，避免同一验证结果被重复使用。
     */
    void consumeVerified(String scene, String email);
}
