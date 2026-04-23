package com.winsalty.quickstart.infra.mail;

/**
 * 通用邮件发送服务。
 * 为各业务模块提供统一的文本/HTML 邮件发送能力。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
public interface MailService {

    void send(MailSendRequest request);

    default void sendText(String to, String subject, String content) {
        send(MailSendRequest.text(to, subject, content));
    }

    default void sendHtml(String to, String subject, String content) {
        send(MailSendRequest.html(to, subject, content));
    }
}
