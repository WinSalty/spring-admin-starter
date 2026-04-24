package com.winsalty.quickstart.infra.verification;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 通用邮箱验证码配置。
 * 管理验证码开关、有效期、长度、错误次数限制和默认邮件文案。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.mail.verification-code")
public class EmailVerificationCodeProperties {

    /**
     * 是否启用通用邮箱验证码服务。
     */
    private boolean enabled = true;

    /**
     * 数字验证码长度。
     */
    private int codeLength = 6;

    /**
     * 验证码有效期，单位秒。
     */
    private long ttlSeconds = 300L;

    /**
     * verifyCode 成功后的已验证状态有效期，单位秒。
     */
    private long verifiedTtlSeconds = 600L;

    /**
     * 单个场景和邮箱在有效期内允许的最大错误次数。
     */
    private long failLimit = 5L;

    /**
     * 未指定业务主题时使用的默认邮件主题。
     */
    private String subject = "邮箱验证码";

    /**
     * 未指定业务标题时使用的默认邮件标题。
     */
    private String title = "邮箱验证码";
}
