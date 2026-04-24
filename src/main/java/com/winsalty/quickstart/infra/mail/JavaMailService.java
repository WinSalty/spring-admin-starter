package com.winsalty.quickstart.infra.mail;

import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * 基于 Spring Mail 的通用邮件发送实现。
 * 提供文本邮件和 HTML 邮件的统一异步发送能力，供各业务模块复用。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Service
@ConditionalOnProperty(prefix = "app.mail.aliyun", name = "enabled", havingValue = "false", matchIfMissing = true)
public class JavaMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(JavaMailService.class);

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;
    private final Executor mailTaskExecutor;

    public JavaMailService(JavaMailSender javaMailSender,
                           MailProperties mailProperties,
                           @Qualifier("mailTaskExecutor") Executor mailTaskExecutor) {
        this.javaMailSender = javaMailSender;
        this.mailProperties = mailProperties;
        this.mailTaskExecutor = mailTaskExecutor;
    }

    @Override
    public void send(MailSendRequest request) {
        MailSendSupport.validateRequest(mailProperties, request);
        validateSmtpConfig();
        boolean html = MailSendSupport.hasHtmlContent(request);
        String maskedTo = MailSendSupport.maskEmail(request.getTo());
        String subjectFingerprint = MailSendSupport.fingerprint(request.getSubject());
        try {
            MimeMessage mimeMessage = buildMimeMessage(request);
            mailTaskExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    sendMimeMessage(mimeMessage, maskedTo, subjectFingerprint, html);
                }
            });
            log.info("mail send task accepted, to={}, subjectFingerprint={}, html={}", maskedTo, subjectFingerprint, html);
        } catch (MessagingException exception) {
            log.error("mail message build failed, to={}, subjectFingerprint={}", maskedTo, subjectFingerprint, exception);
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED, "邮件内容构建失败");
        } catch (RejectedExecutionException exception) {
            log.error("mail send task rejected, to={}, subjectFingerprint={}", maskedTo, subjectFingerprint, exception);
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED, "邮件发送队列繁忙，请稍后重试");
        }
    }

    private MimeMessage buildMimeMessage(MailSendRequest request) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        boolean multipart = MailSendSupport.hasHtmlContent(request) && MailSendSupport.hasTextContent(request);
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, multipart,
                MailSendSupport.resolveEncoding(mailProperties));
        helper.setFrom(MailSendSupport.parseSingleAddress(MailSendSupport.resolveFrom(mailProperties), "邮件发件人格式不正确"));
        helper.setTo(MailSendSupport.parseSingleAddress(request.getTo().trim(), "邮件收件人格式不正确"));
        helper.setSubject(request.getSubject().trim());
        applyContent(helper, request);
        return mimeMessage;
    }

    private void sendMimeMessage(MimeMessage mimeMessage, String maskedTo, String subjectFingerprint, boolean html) {
        try {
            javaMailSender.send(mimeMessage);
            log.info("mail sent success, to={}, subjectFingerprint={}, html={}", maskedTo, subjectFingerprint, html);
        } catch (MailException exception) {
            log.error("mail send failed, to={}, subjectFingerprint={}", maskedTo, subjectFingerprint, exception);
        }
    }

    private void validateSmtpConfig() {
        if (MailSendSupport.resolveFrom(mailProperties) == null) {
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED, "邮件发件人未配置");
        }
    }

    private void applyContent(MimeMessageHelper helper, MailSendRequest request) throws MessagingException {
        if (MailSendSupport.hasTextContent(request) && MailSendSupport.hasHtmlContent(request)) {
            helper.setText(request.getTextContent().trim(), request.getHtmlContent().trim());
            return;
        }
        if (MailSendSupport.hasHtmlContent(request)) {
            helper.setText(request.getHtmlContent().trim(), true);
            return;
        }
        helper.setText(request.getTextContent().trim(), false);
    }
}
