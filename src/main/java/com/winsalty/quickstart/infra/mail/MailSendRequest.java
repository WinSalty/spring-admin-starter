package com.winsalty.quickstart.infra.mail;

/**
 * 通用邮件发送请求。
 * 用于封装收件人、主题、正文、格式和可选发件人。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
public class MailSendRequest {

    private String to;
    private String subject;
    private String content;
    private boolean html;
    private String from;

    public static MailSendRequest text(String to, String subject, String content) {
        MailSendRequest request = new MailSendRequest();
        request.setTo(to);
        request.setSubject(subject);
        request.setContent(content);
        request.setHtml(false);
        return request;
    }

    public static MailSendRequest html(String to, String subject, String content) {
        MailSendRequest request = new MailSendRequest();
        request.setTo(to);
        request.setSubject(subject);
        request.setContent(content);
        request.setHtml(true);
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isHtml() {
        return html;
    }

    public void setHtml(boolean html) {
        this.html = html;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
