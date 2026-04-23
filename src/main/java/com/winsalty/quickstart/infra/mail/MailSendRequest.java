package com.winsalty.quickstart.infra.mail;

/**
 * 通用邮件发送请求。
 * 用于封装收件人、主题、纯文本/HTML 正文和可选发件人。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
public class MailSendRequest {

    private String to;
    private String subject;
    private String textContent;
    private String htmlContent;
    private String from;

    public static MailSendRequest text(String to, String subject, String content) {
        MailSendRequest request = new MailSendRequest();
        request.setTo(to);
        request.setSubject(subject);
        request.setTextContent(content);
        return request;
    }

    public static MailSendRequest html(String to, String subject, String content) {
        MailSendRequest request = new MailSendRequest();
        request.setTo(to);
        request.setSubject(subject);
        request.setHtmlContent(content);
        return request;
    }

    public static MailSendRequest html(String to, String subject, String textContent, String htmlContent) {
        MailSendRequest request = new MailSendRequest();
        request.setTo(to);
        request.setSubject(subject);
        request.setTextContent(textContent);
        request.setHtmlContent(htmlContent);
        return request;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

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

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
