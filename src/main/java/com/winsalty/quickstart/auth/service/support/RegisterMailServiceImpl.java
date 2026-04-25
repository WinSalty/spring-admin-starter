package com.winsalty.quickstart.auth.service.support;

import com.winsalty.quickstart.auth.config.RegisterMailProperties;
import com.winsalty.quickstart.infra.mail.MailService;
import com.winsalty.quickstart.infra.mail.MailTemplateContent;
import com.winsalty.quickstart.infra.mail.MailTemplateService;
import com.winsalty.quickstart.infra.mail.StandardMailTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 注册账号激活邮件发送服务实现。
 * 负责注册激活场景的主题、正文模板拼装，并复用通用邮件服务发送。
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
    public void sendVerificationLink(String email, String verificationUrl, long ttlSeconds) {
        // 注册激活邮件统一走标准模板，文本和 HTML 两份内容同时生成，兼容禁用 HTML 的邮箱客户端。
        MailTemplateContent templateContent = mailTemplateService.renderStandard(buildTemplate(verificationUrl, ttlSeconds));
        mailService.sendHtml(email, resolveSubject(), templateContent.getTextContent(), templateContent.getHtmlContent());
    }

    private String resolveSubject() {
        return StringUtils.hasText(registerMailProperties.getSubject())
                ? registerMailProperties.getSubject().trim()
                : "Spring Admin 账号激活";
    }

    private StandardMailTemplate buildTemplate(String verificationUrl, long ttlSeconds) {
        // 邮件里展示分钟级有效期，比秒级更适合用户阅读；不足一分钟按一分钟提示。
        long ttlMinutes = Math.max(1L, ttlSeconds / 60L);
        StandardMailTemplate template = new StandardMailTemplate();
        template.setTitle("验证邮箱，完成注册");
        template.setGreeting("您好，");
        template.setSummary("你正在创建后台账号，请点击下方按钮验证邮箱。验证完成后即可登录后台。");
        template.setDescription("本链接将在 " + ttlMinutes + " 分钟内有效，且仅能用于本次账号激活。");
        template.setActionText("验证邮箱并激活账号");
        template.setActionUrl(verificationUrl);
        template.setActionFallbackTip("如果按钮无法打开，请复制下方链接到浏览器地址栏完成邮箱验证。");
        template.setFooterNote("如果这不是你的操作，请直接忽略本邮件，无需进行任何处理。");
        return template;
    }
}
