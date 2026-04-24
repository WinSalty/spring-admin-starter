package com.winsalty.quickstart.infra.mail;

import com.winsalty.quickstart.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通用邮件发送服务测试。
 * 覆盖系统发件人约束、单收件人校验和异步执行器派发发送逻辑。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
class JavaMailServiceTest {

    private static final String SYSTEM_FROM = "noreply@example.com";
    private static final String TO_EMAIL = "test@example.com";
    private static final String SUBJECT = "注册验证码";
    private static final String TEXT_CONTENT = "验证码 123456";

    @Test
    void sendRejectsBusinessFromOverride() {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        JavaMailService service = new JavaMailService(javaMailSender, enabledMailProperties(), Runnable::run);
        MailSendRequest request = MailSendRequest.text(TO_EMAIL, SUBJECT, TEXT_CONTENT);
        request.setFrom("spoof@example.com");

        assertThrows(BusinessException.class, () -> service.send(request));

        verify(javaMailSender, never()).createMimeMessage();
    }

    @Test
    void sendRejectsMultipleRecipients() {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        when(javaMailSender.createMimeMessage()).thenReturn(newMimeMessage());
        JavaMailService service = new JavaMailService(javaMailSender, enabledMailProperties(), Runnable::run);
        MailSendRequest request = MailSendRequest.text("first@example.com,second@example.com", SUBJECT, TEXT_CONTENT);

        assertThrows(BusinessException.class, () -> service.send(request));

        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendUsesConfiguredFromAndExecutor() throws Exception {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = newMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        JavaMailService service = new JavaMailService(javaMailSender, enabledMailProperties(), Runnable::run);

        service.send(MailSendRequest.text(TO_EMAIL, SUBJECT, TEXT_CONTENT));

        verify(javaMailSender).send(mimeMessage);
        assertEquals(SYSTEM_FROM, ((InternetAddress) mimeMessage.getFrom()[0]).getAddress());
        assertEquals(TO_EMAIL, ((InternetAddress) mimeMessage.getAllRecipients()[0]).getAddress());
    }

    private MailProperties enabledMailProperties() {
        MailProperties properties = new MailProperties();
        properties.setEnabled(true);
        properties.setFrom(SYSTEM_FROM);
        return properties;
    }

    private MimeMessage newMimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }
}
