package com.winsalty.quickstart.infra.mail;

/**
 * 邮件模板渲染结果。
 * 同时承载 HTML 内容和纯文本兜底内容。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
public class MailTemplateContent {

    private String textContent;
    private String htmlContent;

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }
}
