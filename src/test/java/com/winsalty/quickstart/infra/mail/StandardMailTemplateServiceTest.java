package com.winsalty.quickstart.infra.mail;

import com.winsalty.quickstart.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 标准邮件模板服务测试。
 * 覆盖模板外链协议、CSS 颜色配置和用户输入 HTML 转义逻辑。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
class StandardMailTemplateServiceTest {

    @Test
    void renderStandardRejectsUnsafeActionUrl() {
        StandardMailTemplateService service = new StandardMailTemplateService(new MailProperties());
        StandardMailTemplate template = baseTemplate();
        template.setActionText("查看详情");
        template.setActionUrl("javascript:alert(1)");

        assertThrows(BusinessException.class, () -> service.renderStandard(template));
    }

    @Test
    void renderStandardRejectsHttpUrlWithoutHost() {
        StandardMailTemplateService service = new StandardMailTemplateService(new MailProperties());
        StandardMailTemplate template = baseTemplate();
        template.setActionText("查看详情");
        template.setActionUrl("http:example.com");

        assertThrows(BusinessException.class, () -> service.renderStandard(template));
    }

    @Test
    void renderStandardRejectsInvalidTemplateColor() {
        MailProperties properties = new MailProperties();
        properties.getTemplate().setPrimaryColor("red;background:url(javascript:alert(1))");
        StandardMailTemplateService service = new StandardMailTemplateService(properties);

        assertThrows(BusinessException.class, () -> service.renderStandard(baseTemplate()));
    }

    @Test
    void renderStandardEscapesUserContent() {
        StandardMailTemplateService service = new StandardMailTemplateService(new MailProperties());
        StandardMailTemplate template = baseTemplate();
        template.setTitle("<b>注册</b>");
        template.setSummary("<script>alert('x')</script>");
        template.setDescription("请确认 \"邮箱\" 与 <账号>");

        MailTemplateContent content = service.renderStandard(template);

        assertTrue(content.getHtmlContent().contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;"));
        assertTrue(content.getHtmlContent().contains("请确认 &quot;邮箱&quot; 与 &lt;账号&gt;"));
        assertFalse(content.getHtmlContent().contains("<script>alert"));
    }

    @Test
    void renderStandardCentersActionButtonAndShowsClickableFallbackLink() {
        StandardMailTemplateService service = new StandardMailTemplateService(new MailProperties());
        StandardMailTemplate template = baseTemplate();
        template.setActionText("验证邮箱");
        template.setActionUrl("https://example.com/register/verify-email?email=test@example.com&token=abc");
        template.setActionFallbackTip("如果按钮无法打开，请复制下方链接到浏览器地址栏完成邮箱验证。");

        MailTemplateContent content = service.renderStandard(template);

        assertTrue(content.getHtmlContent().contains("margin:26px 0 12px;text-align:center;"));
        assertTrue(content.getHtmlContent().contains("如果按钮无法打开，请复制下方链接到浏览器地址栏完成邮箱验证。"));
        assertTrue(content.getHtmlContent().contains("<a href=\"https://example.com/register/verify-email?email=test@example.com&amp;token=abc\""));
        assertTrue(content.getTextContent().contains("如果按钮无法打开，请复制下方链接到浏览器地址栏完成邮箱验证。"));
    }

    private StandardMailTemplate baseTemplate() {
        StandardMailTemplate template = new StandardMailTemplate();
        template.setTitle("完成注册校验");
        template.setGreeting("您好，");
        template.setSummary("请输入验证码完成注册。");
        template.setHighlightLabel("验证码");
        template.setHighlightValue("123456");
        return template;
    }
}
