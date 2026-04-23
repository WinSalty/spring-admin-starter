package com.winsalty.quickstart.infra.mail;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 标准邮件模板服务实现。
 * 生成项目统一的卡片式 HTML 邮件，并输出纯文本版本用于兼容性兜底。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Service
public class StandardMailTemplateService implements MailTemplateService {

    private final MailProperties mailProperties;

    public StandardMailTemplateService(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    @Override
    public MailTemplateContent renderStandard(StandardMailTemplate template) {
        MailTemplateContent content = new MailTemplateContent();
        content.setTextContent(buildTextContent(template));
        content.setHtmlContent(buildHtmlContent(template));
        return content;
    }

    private String buildTextContent(StandardMailTemplate template) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, resolveText(template.getGreeting(), "您好，"));
        appendBlankLine(builder);
        appendLine(builder, resolveText(template.getSummary(), ""));
        appendBlankLine(builder);
        if (StringUtils.hasText(template.getHighlightLabel()) || StringUtils.hasText(template.getHighlightValue())) {
            appendLine(builder, resolveText(template.getHighlightLabel(), "关键信息") + "："
                    + resolveText(template.getHighlightValue(), ""));
            appendBlankLine(builder);
        }
        if (StringUtils.hasText(template.getDescription())) {
            appendLine(builder, template.getDescription().trim());
            appendBlankLine(builder);
        }
        if (StringUtils.hasText(template.getActionText()) && StringUtils.hasText(template.getActionUrl())) {
            appendLine(builder, template.getActionText().trim() + "：");
            appendLine(builder, template.getActionUrl().trim());
            appendBlankLine(builder);
        }
        if (StringUtils.hasText(template.getFooterNote())) {
            appendLine(builder, template.getFooterNote().trim());
            appendBlankLine(builder);
        }
        appendLine(builder, resolveSignature());
        return builder.toString().trim();
    }

    private String buildHtmlContent(StandardMailTemplate template) {
        String brandName = escapeHtml(resolveBrandName());
        String primaryColor = escapeHtml(mailProperties.getTemplate().getPrimaryColor());
        String backgroundColor = escapeHtml(mailProperties.getTemplate().getBackgroundColor());
        String cardBackgroundColor = escapeHtml(mailProperties.getTemplate().getCardBackgroundColor());
        StringBuilder builder = new StringBuilder(2048);
        builder.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("<title>").append(escapeHtml(resolveText(template.getTitle(), brandName))).append("</title>")
                .append("</head><body style=\"margin:0;padding:0;background:")
                .append(backgroundColor)
                .append(";font-family:'PingFang SC','Microsoft YaHei',Arial,sans-serif;color:#1f2937;\">")
                .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background:")
                .append(backgroundColor).append(";padding:24px 12px;\">")
                .append("<tr><td align=\"center\">")
                .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width:640px;background:")
                .append(cardBackgroundColor)
                .append(";border-radius:20px;overflow:hidden;box-shadow:0 12px 36px rgba(15,23,42,0.10);\">")
                .append("<tr><td style=\"padding:28px 32px;background:linear-gradient(135deg,")
                .append(primaryColor).append(" 0%,#0f172a 100%);color:#ffffff;\">")
                .append("<div style=\"font-size:13px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.86;\">")
                .append(brandName).append("</div>")
                .append("<div style=\"margin-top:12px;font-size:28px;font-weight:700;line-height:1.35;\">")
                .append(escapeHtml(resolveText(template.getTitle(), brandName))).append("</div>")
                .append("</td></tr>")
                .append("<tr><td style=\"padding:32px;\">")
                .append("<div style=\"font-size:16px;line-height:1.8;color:#111827;\">")
                .append(formatParagraph(resolveText(template.getGreeting(), "您好，")))
                .append(formatParagraph(resolveText(template.getSummary(), "")));
        if (StringUtils.hasText(template.getHighlightValue())) {
            builder.append("<div style=\"margin:28px 0;padding:20px 24px;border-radius:18px;")
                    .append("background:#eff6ff;border:1px solid #bfdbfe;text-align:center;\">")
                    .append("<div style=\"font-size:12px;letter-spacing:0.08em;text-transform:uppercase;color:")
                    .append(primaryColor).append(";font-weight:700;\">")
                    .append(escapeHtml(resolveText(template.getHighlightLabel(), "关键信息")))
                    .append("</div>")
                    .append("<div style=\"margin-top:10px;font-size:34px;line-height:1.2;font-weight:800;")
                    .append("color:#0f172a;letter-spacing:0.18em;\">")
                    .append(escapeHtml(template.getHighlightValue().trim()))
                    .append("</div></div>");
        }
        if (StringUtils.hasText(template.getDescription())) {
            builder.append(formatParagraph(template.getDescription().trim()));
        }
        if (StringUtils.hasText(template.getActionText()) && StringUtils.hasText(template.getActionUrl())) {
            builder.append("<div style=\"margin:28px 0 12px;\">")
                    .append("<a href=\"").append(escapeHtmlAttribute(template.getActionUrl().trim()))
                    .append("\" style=\"display:inline-block;padding:14px 24px;border-radius:999px;")
                    .append("background:").append(primaryColor)
                    .append(";color:#ffffff;text-decoration:none;font-size:14px;font-weight:700;\">")
                    .append(escapeHtml(template.getActionText().trim()))
                    .append("</a></div>")
                    .append("<div style=\"font-size:12px;color:#6b7280;line-height:1.7;word-break:break-all;\">")
                    .append(escapeHtml(template.getActionUrl().trim()))
                    .append("</div>");
        }
        if (StringUtils.hasText(template.getFooterNote())) {
            builder.append("<div style=\"margin-top:24px;padding-top:20px;border-top:1px solid #e5e7eb;")
                    .append("font-size:13px;line-height:1.8;color:#6b7280;\">")
                    .append(escapeHtml(template.getFooterNote().trim()))
                    .append("</div>");
        }
        builder.append("<div style=\"margin-top:24px;font-size:13px;color:#6b7280;line-height:1.8;\">")
                .append(escapeHtml(resolveSignature()))
                .append("</div></div></td></tr></table></td></tr></table></body></html>");
        return builder.toString();
    }

    private String formatParagraph(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return "<p style=\"margin:0 0 16px;\">" + escapeHtml(text.trim()) + "</p>";
    }

    private void appendLine(StringBuilder builder, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        builder.append(value.trim()).append('\n');
    }

    private void appendBlankLine(StringBuilder builder) {
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
            builder.append('\n');
        }
        builder.append('\n');
    }

    private String resolveBrandName() {
        return resolveText(mailProperties.getTemplate().getBrandName(), "Spring Admin");
    }

    private String resolveSignature() {
        return resolveText(mailProperties.getTemplate().getSignature(), "Spring Admin Team");
    }

    private String resolveText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br/>");
    }

    private String escapeHtmlAttribute(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
