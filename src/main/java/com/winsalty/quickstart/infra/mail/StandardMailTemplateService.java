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
        appendLine(builder, resolveText(template.getTitle(), resolveBrandName()));
        appendBlankLine(builder);
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
        String primarySoftColor = "#e6f4ff";
        String primaryBorderColor = "#d6e4ff";
        String panelBackgroundColor = "#f7fbff";
        String textPrimaryColor = "#262626";
        String textSecondaryColor = "#595959";
        String textMutedColor = "#8c8c8c";
        StringBuilder builder = new StringBuilder(2048);
        builder.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("<title>").append(escapeHtml(resolveText(template.getTitle(), brandName))).append("</title>")
                .append("</head><body style=\"margin:0;padding:0;background:")
                .append(backgroundColor)
                .append(";font-family:-apple-system,BlinkMacSystemFont,'SF Pro Text','PingFang SC','Microsoft YaHei',Arial,sans-serif;color:")
                .append(textPrimaryColor).append(";\">")
                .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background:")
                .append(backgroundColor).append(";padding:24px 12px;\">")
                .append("<tr><td align=\"center\">")
                .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width:640px;background:")
                .append(cardBackgroundColor)
                .append(";border:1px solid ").append(primaryBorderColor)
                .append(";border-radius:18px;overflow:hidden;box-shadow:0 20px 48px rgba(22,119,255,0.12);\">")
                .append("<tr><td style=\"padding:0 0 28px;background:")
                .append(cardBackgroundColor).append(";\">")
                .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background:")
                .append(panelBackgroundColor)
                .append(";background-image:linear-gradient(145deg,#eef5ff 0%,#f5f7fb 54%,#ffffff 100%);\">")
                .append("<tr><td style=\"padding:30px 32px 26px;\">")
                .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">")
                .append("<tr><td style=\"vertical-align:top;\">")
                .append("<div style=\"display:inline-block;width:52px;height:52px;line-height:52px;text-align:center;")
                .append("border-radius:14px;background:linear-gradient(135deg,")
                .append(primaryColor).append(",#4096ff);color:#ffffff;font-size:18px;font-weight:700;")
                .append("box-shadow:0 18px 36px rgba(22,119,255,0.24);\">")
                .append(resolveBrandMark(brandName)).append("</div>")
                .append("<div style=\"margin-top:24px;\">")
                .append("<span style=\"display:inline-block;border:1px solid ").append(primaryBorderColor)
                .append(";border-radius:999px;background:").append(primarySoftColor)
                .append(";color:").append(primaryColor)
                .append(";font-size:12px;font-weight:600;line-height:24px;padding:0 10px;\">")
                .append(brandName).append("</span>")
                .append("</div>")
                .append("<div style=\"margin-top:18px;font-size:30px;font-weight:700;line-height:1.33;color:")
                .append(textPrimaryColor).append(";\">")
                .append(escapeHtml(resolveText(template.getTitle(), brandName))).append("</div>")
                .append("<div style=\"margin-top:12px;font-size:15px;line-height:1.8;color:")
                .append(textSecondaryColor).append(";max-width:460px;\">")
                .append(escapeHtml(resolveText(template.getSummary(), ""))).append("</div>")
                .append("</td></tr></table>")
                .append("</td></tr>")
                .append("</table>")
                .append("<div style=\"padding:0 32px;\">")
                .append("<div style=\"font-size:15px;line-height:1.85;color:")
                .append(textPrimaryColor).append(";\">")
                .append(formatParagraph(resolveText(template.getGreeting(), "您好，"), textPrimaryColor, 18));
        if (StringUtils.hasText(template.getHighlightValue())) {
            builder.append("<div style=\"margin:26px 0 22px;padding:22px 24px;border-radius:16px;")
                    .append("background:#f0f5ff;border:1px solid ").append(primaryBorderColor)
                    .append(";text-align:center;\">")
                    .append("<div style=\"font-size:12px;letter-spacing:0.04em;color:")
                    .append(primaryColor).append(";font-weight:600;\">")
                    .append(escapeHtml(resolveText(template.getHighlightLabel(), "关键信息")))
                    .append("</div>")
                    .append("<div style=\"margin-top:12px;font-size:34px;line-height:1.2;font-weight:700;")
                    .append("color:").append(textPrimaryColor)
                    .append(";letter-spacing:0.22em;font-family:'SFMono-Regular',Consolas,'Liberation Mono',Menlo,monospace;\">")
                    .append(escapeHtml(template.getHighlightValue().trim()))
                    .append("</div></div>");
        }
        if (StringUtils.hasText(template.getDescription())) {
            builder.append("<div style=\"border:1px solid #f0f0f0;border-radius:16px;background:#fafcff;")
                    .append("padding:18px 20px;margin-top:0;\">")
                    .append("<div style=\"font-size:12px;color:").append(primaryColor)
                    .append(";font-weight:600;margin-bottom:8px;\">校验说明</div>")
                    .append("<div style=\"font-size:14px;line-height:1.85;color:").append(textSecondaryColor).append(";\">")
                    .append(escapeHtml(template.getDescription().trim()))
                    .append("</div></div>");
        }
        if (StringUtils.hasText(template.getActionText()) && StringUtils.hasText(template.getActionUrl())) {
            builder.append("<div style=\"margin:26px 0 12px;\">")
                    .append("<a href=\"").append(escapeHtmlAttribute(template.getActionUrl().trim()))
                    .append("\" style=\"display:inline-block;padding:13px 24px;border-radius:10px;")
                    .append("background:").append(primaryColor)
                    .append(";color:#ffffff;text-decoration:none;font-size:14px;font-weight:600;\">")
                    .append(escapeHtml(template.getActionText().trim()))
                    .append("</a></div>")
                    .append("<div style=\"font-size:12px;color:").append(textMutedColor)
                    .append(";line-height:1.75;word-break:break-all;\">")
                    .append(escapeHtml(template.getActionUrl().trim()))
                    .append("</div>");
        }
        if (StringUtils.hasText(template.getFooterNote())) {
            builder.append("<div style=\"margin-top:24px;padding-top:18px;border-top:1px solid #f0f0f0;")
                    .append("font-size:13px;line-height:1.8;color:").append(textMutedColor).append(";\">")
                    .append(escapeHtml(template.getFooterNote().trim()))
                    .append("</div>");
        }
        builder.append("<div style=\"margin-top:24px;font-size:13px;color:").append(textMutedColor)
                .append(";line-height:1.8;\">")
                .append(escapeHtml(resolveSignature()))
                .append("</div></div></div></td></tr></table></td></tr></table></body></html>");
        return builder.toString();
    }

    private String formatParagraph(String text, String color, int marginBottom) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return "<p style=\"margin:0 0 " + marginBottom + "px;color:" + color + ";\">"
                + escapeHtml(text.trim()) + "</p>";
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
        return resolveText(mailProperties.getTemplate().getBrandName(), "React Admin Starter");
    }

    private String resolveSignature() {
        return resolveText(mailProperties.getTemplate().getSignature(), "React Admin Starter");
    }

    private String resolveBrandMark(String brandName) {
        String raw = brandName == null ? "" : brandName.replaceAll("[^A-Za-z0-9]", "");
        if (raw.length() >= 2) {
            return escapeHtml(raw.substring(0, 2).toUpperCase());
        }
        if (raw.length() == 1) {
            return escapeHtml((raw + "A").toUpperCase());
        }
        return "RA";
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
