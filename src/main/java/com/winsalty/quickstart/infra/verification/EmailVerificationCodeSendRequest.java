package com.winsalty.quickstart.infra.verification;

import lombok.Data;

/**
 * 邮箱验证码发送请求。
 * 由业务模块传入场景编码、收件邮箱和可选邮件文案，实现验证码能力复用。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class EmailVerificationCodeSendRequest {

    /**
     * 业务场景编码，例如 password-reset、change-email。
     */
    private String scene;

    /**
     * 收件邮箱。
     */
    private String email;

    /**
     * 邮件主题，不传时使用通用默认主题。
     */
    private String subject;

    /**
     * 邮件标题，不传时使用通用默认标题。
     */
    private String title;

    /**
     * 邮件问候语。
     */
    private String greeting;

    /**
     * 邮件摘要说明。
     */
    private String summary;

    /**
     * 验证码下方的补充说明。
     */
    private String description;

    /**
     * 邮件底部提示。
     */
    private String footerNote;
}
