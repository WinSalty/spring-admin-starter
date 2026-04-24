package com.winsalty.quickstart.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 注册邮箱验证邮件配置。
 * 只管理注册场景专属模板和开关，通用发件人配置由 app.mail 统一维护。
 * 创建日期：2026-04-19
 * author：sunshengxian
 */
@Component
@ConfigurationProperties(prefix = "app.mail.register")
@Data
public class RegisterMailProperties {

    private boolean enabled = true;
    private String subject = "Spring Admin 注册验证";
    private String verifyLinkBaseUrl = "http://localhost:5173";
}
