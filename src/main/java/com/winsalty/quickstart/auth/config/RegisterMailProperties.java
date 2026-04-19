package com.winsalty.quickstart.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 注册验证码邮件配置。
 * 由环境变量注入 SMTP 发件账号、发件人和邮件主题。
 * 创建日期：2026-04-19
 * author：sunshengxian
 */
@Component
@ConfigurationProperties(prefix = "app.mail.register")
@Data
public class RegisterMailProperties {

    private boolean enabled = true;
    private String from;
    private String subject = "Spring Admin 注册验证码";
}
