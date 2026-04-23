package com.winsalty.quickstart.auth.service.support;

import com.winsalty.quickstart.auth.config.RegisterMailProperties;
import com.winsalty.quickstart.infra.mail.MailSendRequest;
import com.winsalty.quickstart.infra.mail.MailService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 注册验证码邮件发送服务实现。
 * 负责注册场景的主题、正文模板拼装，并复用通用邮件服务发送。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Service
public class RegisterMailServiceImpl implements RegisterMailService {

    private final MailService mailService;
    private final RegisterMailProperties registerMailProperties;

    public RegisterMailServiceImpl(MailService mailService, RegisterMailProperties registerMailProperties) {
        this.mailService = mailService;
        this.registerMailProperties = registerMailProperties;
    }

    @Override
    public boolean isEnabled() {
        return registerMailProperties.isEnabled();
    }

    @Override
    public void sendVerifyCode(String email, String code, long ttlSeconds) {
        mailService.send(MailSendRequest.text(email, resolveSubject(), buildContent(code, ttlSeconds)));
    }

    private String resolveSubject() {
        return StringUtils.hasText(registerMailProperties.getSubject())
                ? registerMailProperties.getSubject().trim()
                : "Spring Admin 注册验证码";
    }

    private String buildContent(String code, long ttlSeconds) {
        long ttlMinutes = Math.max(1L, ttlSeconds / 60L);
        // 邮件正文保持纯文本，兼容大多数企业邮箱和简单 SMTP 服务。
        return "您好，\n\n"
                + "您正在注册 Spring Admin Starter，邮箱验证码为：\n\n"
                + code + "\n\n"
                + "验证码 " + ttlMinutes + " 分钟内有效，请勿泄露给他人。\n"
                + "如非本人操作，请忽略本邮件。";
    }
}
