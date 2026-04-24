package com.winsalty.quickstart.infra.mail;

import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * 标准邮件模板服务实现。
 * 生成项目统一的卡片式 HTML 邮件，并输出纯文本版本用于兼容性兜底。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Service
public class StandardMailTemplateService implements MailTemplateService {

    private static final String DEFAULT_BRAND_NAME = "React Admin Starter";
    private static final String DEFAULT_GREETING = "您好，";
    private static final String DEFAULT_HIGHLIGHT_LABEL = "关键信息";
    private static final String DEFAULT_ACTION_FALLBACK_TIP = "如果按钮无法打开，可复制下方链接到浏览器地址栏打开。";
    private static final String DEFAULT_PRIMARY_COLOR = "#1677ff";
    private static final String DEFAULT_BACKGROUND_COLOR = "#f4f7fb";
    private static final String DEFAULT_CARD_BACKGROUND_COLOR = "#ffffff";
    private static final String PRIMARY_SOFT_COLOR = "#e6f4ff";
    private static final String PRIMARY_BORDER_COLOR = "#d6e4ff";
    private static final String CARD_BORDER_COLOR = "#e6edf7";
    private static final String PANEL_BACKGROUND_COLOR = "#f7fbff";
    private static final String TEXT_PRIMARY_COLOR = "#262626";
    private static final String TEXT_SECONDARY_COLOR = "#595959";
    private static final String TEXT_MUTED_COLOR = "#8c8c8c";
    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

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
        appendLine(builder, resolveText(template.getGreeting(), DEFAULT_GREETING));
        appendBlankLine(builder);
        appendLine(builder, resolveText(template.getSummary(), ""));
        appendBlankLine(builder);
        if (StringUtils.hasText(template.getHighlightLabel()) || StringUtils.hasText(template.getHighlightValue())) {
            appendLine(builder, resolveText(template.getHighlightLabel(), DEFAULT_HIGHLIGHT_LABEL) + "："
                    + resolveText(template.getHighlightValue(), ""));
            appendBlankLine(builder);
        }
        if (StringUtils.hasText(template.getDescription())) {
            appendLine(builder, template.getDescription().trim());
            appendBlankLine(builder);
        }
        String actionUrl = StringUtils.hasText(template.getActionText()) ? resolveSafeActionUrl(template.getActionUrl()) : null;
        if (StringUtils.hasText(template.getActionText()) && StringUtils.hasText(actionUrl)) {
            appendLine(builder, template.getActionText().trim() + "：");
            appendLine(builder, resolveText(template.getActionFallbackTip(), DEFAULT_ACTION_FALLBACK_TIP));
            appendLine(builder, actionUrl);
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
        String primaryColor = resolveCssColor(mailProperties.getTemplate().getPrimaryColor(), DEFAULT_PRIMARY_COLOR);
        String backgroundColor = resolveCssColor(mailProperties.getTemplate().getBackgroundColor(), DEFAULT_BACKGROUND_COLOR);
        String cardBackgroundColor = resolveCssColor(mailProperties.getTemplate().getCardBackgroundColor(), DEFAULT_CARD_BACKGROUND_COLOR);
        StringBuilder builder = new StringBuilder(2048);
        builder.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("<title>").append(escapeHtml(resolveText(template.getTitle(), brandName))).append("</title>")
                .append("</head><body style=\"margin:0;padding:0;background:")
                .append(backgroundColor)
                .append(";font-family:-apple-system,BlinkMacSystemFont,'SF Pro Text','PingFang SC','Microsoft YaHei',Arial,sans-serif;color:")
                .append(TEXT_PRIMARY_COLOR).append(";\">")
                .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background:")
                .append(backgroundColor).append(";padding:24px 12px;\">")
                .append("<tr><td align=\"center\">")
                .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width:640px;background:")
                .append(cardBackgroundColor)
                .append(";border:1px solid ").append(CARD_BORDER_COLOR)
                .append(";border-radius:18px;overflow:hidden;box-shadow:0 22px 52px rgba(24,39,75,0.12);\">")
                .append("<tr><td style=\"padding:0;background:")
                .append(cardBackgroundColor).append(";\">")
                .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background:")
                .append(PANEL_BACKGROUND_COLOR)
                .append(";background-image:linear-gradient(145deg,#eef5ff 0%,#f7fbff 48%,#ffffff 100%);\">")
                .append("<tr><td style=\"padding:24px 28px 20px;\">")
                .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">")
                .append("<tr>")
                .append("<td style=\"vertical-align:middle;\">")
                .append("<div style=\"display:inline-block;width:44px;height:44px;line-height:44px;text-align:center;")
                .append("border-radius:14px;background:linear-gradient(135deg,")
                .append(primaryColor).append(",#36cfc9);color:#ffffff;font-size:16px;font-weight:700;")
                .append("box-shadow:0 16px 30px rgba(22,119,255,0.18);\">")
                .append(resolveBrandMark(brandName)).append("</div>")
                .append("</td>")
                .append("<td align=\"right\" style=\"vertical-align:middle;\">")
                .append("<span style=\"display:inline-block;border:1px solid ").append(PRIMARY_BORDER_COLOR)
                .append(";border-radius:999px;background:").append(PRIMARY_SOFT_COLOR)
                .append(";color:").append(primaryColor)
                .append(";font-size:12px;font-weight:600;line-height:24px;padding:0 10px;\">")
                .append(brandName).append("</span>")
                .append("</td>")
                .append("</tr>")
                .append("<tr><td colspan=\"2\" style=\"padding-top:20px;\">")
                .append("<div style=\"font-size:28px;font-weight:700;line-height:1.32;color:")
                .append(TEXT_PRIMARY_COLOR).append(";\">")
                .append(escapeHtml(resolveText(template.getTitle(), brandName))).append("</div>")
                .append("<div style=\"margin-top:10px;font-size:15px;line-height:1.8;color:")
                .append(TEXT_SECONDARY_COLOR).append(";max-width:460px;\">")
                .append(escapeHtml(resolveText(template.getSummary(), ""))).append("</div>")
                .append("</td></tr></table>")
                .append("</td></tr>")
                .append("</table>")
                .append("<div style=\"padding:24px 28px 28px;\">")
                .append("<div style=\"font-size:15px;line-height:1.85;color:")
                .append(TEXT_PRIMARY_COLOR).append(";\">")
                .append(formatParagraph(resolveText(template.getGreeting(), DEFAULT_GREETING), TEXT_PRIMARY_COLOR, 16));
        if (StringUtils.hasText(template.getHighlightValue())) {
            builder.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" ")
                    .append("style=\"margin:22px 0 18px;border-collapse:separate;\">")
                    .append("<tr>")
                    .append("<td style=\"width:124px;padding:18px 20px;border:1px solid ").append(PRIMARY_BORDER_COLOR)
                    .append(";border-right:none;border-radius:16px 0 0 16px;background:#f7fbff;vertical-align:top;\">")
                    .append("<div style=\"font-size:12px;color:").append(primaryColor)
                    .append(";font-weight:600;line-height:1.7;\">")
                    .append(escapeHtml(resolveText(template.getHighlightLabel(), DEFAULT_HIGHLIGHT_LABEL)))
                    .append("</div>")
                    .append("</td>")
                    .append("<td style=\"padding:18px 22px;border:1px solid ").append(PRIMARY_BORDER_COLOR)
                    .append(";border-radius:0 16px 16px 0;background:#ffffff;\">")
                    .append("<div style=\"font-size:32px;line-height:1.2;font-weight:700;color:")
                    .append(TEXT_PRIMARY_COLOR)
                    .append(";letter-spacing:0.18em;font-family:'SFMono-Regular',Consolas,'Liberation Mono',Menlo,monospace;\">")
                    .append(escapeHtml(template.getHighlightValue().trim()))
                    .append("</div>")
                    .append("</td></tr></table>");
        }
        if (StringUtils.hasText(template.getDescription())) {
            builder.append("<div style=\"font-size:14px;line-height:1.85;color:").append(TEXT_SECONDARY_COLOR)
                    .append(";margin-top:0;\">")
                    .append(escapeHtml(template.getDescription().trim()))
                    .append("</div>");
        }
        String actionUrl = StringUtils.hasText(template.getActionText()) ? resolveSafeActionUrl(template.getActionUrl()) : null;
        if (StringUtils.hasText(template.getActionText()) && StringUtils.hasText(actionUrl)) {
            builder.append("<div style=\"margin:26px 0 12px;text-align:center;\">")
                    .append("<a href=\"").append(escapeHtmlAttribute(actionUrl))
                    .append("\" style=\"display:inline-block;padding:13px 24px;border-radius:10px;")
                    .append("background:").append(primaryColor)
                    .append(";color:#ffffff;text-decoration:none;font-size:14px;font-weight:600;")
                    .append("min-width:180px;text-align:center;box-shadow:0 12px 24px rgba(22,119,255,0.22);\">")
                    .append(escapeHtml(template.getActionText().trim()))
                    .append("</a></div>")
                    .append("<div style=\"font-size:12px;color:").append(TEXT_MUTED_COLOR)
                    .append(";line-height:1.75;background:#f7f9fc;border:1px solid #edf1f7;")
                    .append("border-radius:10px;padding:10px 12px;\">")
                    .append("<div style=\"margin-bottom:6px;\">")
                    .append(escapeHtml(resolveText(template.getActionFallbackTip(), DEFAULT_ACTION_FALLBACK_TIP)))
                    .append("</div>")
                    .append("<a href=\"").append(escapeHtmlAttribute(actionUrl))
                    .append("\" style=\"display:block;color:").append(primaryColor)
                    .append(";text-decoration:underline;word-break:break-all;\">")
                    .append(escapeHtml(actionUrl))
                    .append("</a>")
                    .append("</div>");
        }
        if (StringUtils.hasText(template.getFooterNote())) {
            builder.append("<div style=\"margin-top:22px;padding-top:16px;border-top:1px solid #f0f0f0;")
                    .append("font-size:13px;line-height:1.8;color:").append(TEXT_MUTED_COLOR).append(";\">")
                    .append(escapeHtml(template.getFooterNote().trim()))
                    .append("</div>");
        }
        builder.append("<div style=\"margin-top:22px;font-size:13px;color:").append(TEXT_MUTED_COLOR)
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

    private String resolveCssColor(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        String trimmed = value.trim();
        if (!HEX_COLOR_PATTERN.matcher(trimmed).matches()) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮件模板颜色值不合法");
        }
        return trimmed;
    }

    private String resolveSafeActionUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme();
            if (!HTTP_SCHEME.equalsIgnoreCase(scheme) && !HTTPS_SCHEME.equalsIgnoreCase(scheme)) {
                throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮件跳转链接协议不支持");
            }
            if (!StringUtils.hasText(uri.getHost())) {
                throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮件跳转链接格式不正确");
            }
            return trimmed;
        } catch (URISyntaxException exception) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮件跳转链接格式不正确");
        }
    }

    private String resolveBrandName() {
        return resolveText(mailProperties.getTemplate().getBrandName(), DEFAULT_BRAND_NAME);
    }

    private String resolveSignature() {
        return resolveText(mailProperties.getTemplate().getSignature(), DEFAULT_BRAND_NAME);
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
                .replace("'", "&#39;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
