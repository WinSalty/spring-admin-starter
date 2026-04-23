package com.winsalty.quickstart.auth.service.support;

import com.winsalty.quickstart.auth.config.RegisterMailProperties;
import com.winsalty.quickstart.infra.mail.MailService;
import com.winsalty.quickstart.infra.mail.MailTemplateContent;
import com.winsalty.quickstart.infra.mail.MailTemplateService;
import com.winsalty.quickstart.infra.mail.StandardMailTemplate;
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
    private final MailTemplateService mailTemplateService;
    private final RegisterMailProperties registerMailProperties;

    public RegisterMailServiceImpl(MailService mailService,
                                   MailTemplateService mailTemplateService,
                                   RegisterMailProperties registerMailProperties) {
        this.mailService = mailService;
        this.mailTemplateService = mailTemplateService;
        this.registerMailProperties = registerMailProperties;
    }

    @Override
    public boolean isEnabled() {
        return registerMailProperties.isEnabled();
    }

    @Override
    public void sendVerifyCode(String email, String code, long ttlSeconds) {
        MailTemplateContent templateContent = mailTemplateService.renderStandard(buildTemplate(code, ttlSeconds));
        mailService.sendHtml(email, resolveSubject(), templateContent.getTextContent(), templateContent.getHtmlContent());
    }

    private String resolveSubject() {
        return StringUtils.hasText(registerMailProperties.getSubject())
                ? registerMailProperties.getSubject().trim()
                : "Spring Admin 注册验证码";
    }

    private StandardMailTemplate buildTemplate(String code, long ttlSeconds) {
        long ttlMinutes = Math.max(1L, ttlSeconds / 60L);
        StandardMailTemplate template = new StandardMailTemplate();
        template.setTitle("完成注册校验");
        template.setGreeting("您好，");
        template.setSummary("你正在创建后台账号，请使用下方验证码完成本次注册校验，继续进入系统。");
        template.setHighlightLabel("本次验证码");
        template.setHighlightValue(code);
        template.setDescription("验证码在 " + ttlMinutes + " 分钟内有效，仅用于本次注册操作。为保证账号安全，请勿将验证码透露给他人。");
        template.setFooterNote("如果这不是你的操作，请直接忽略本邮件，无需进行任何处理。");
        return template;
    }
}
