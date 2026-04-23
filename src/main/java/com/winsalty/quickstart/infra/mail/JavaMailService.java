package com.winsalty.quickstart.infra.mail;

import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * 基于 Spring Mail 的通用邮件发送实现。
 * 提供文本邮件和 HTML 邮件的统一发送能力，供各业务模块复用。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Service
public class JavaMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(JavaMailService.class);

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;

    public JavaMailService(JavaMailSender javaMailSender, MailProperties mailProperties) {
        this.javaMailSender = javaMailSender;
        this.mailProperties = mailProperties;
    }

    @Override
    public void send(MailSendRequest request) {
        validateRequest(request);
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, resolveEncoding());
            helper.setFrom(resolveFrom(request));
            helper.setTo(request.getTo().trim());
            helper.setSubject(request.getSubject().trim());
            helper.setText(request.getContent(), request.isHtml());
            javaMailSender.send(mimeMessage);
            log.info("mail sent success, to={}, subject={}, html={}", request.getTo().trim(), request.getSubject().trim(), request.isHtml());
        } catch (MessagingException exception) {
            log.error("mail message build failed, to={}, subject={}", safeTrim(request.getTo()), safeTrim(request.getSubject()), exception);
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED, "邮件内容构建失败");
        } catch (MailException exception) {
            log.error("mail send failed, to={}, subject={}, message={}",
                    safeTrim(request.getTo()), safeTrim(request.getSubject()), exception.getMessage(), exception);
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED, "邮件发送失败，请稍后重试");
        }
    }

    private void validateRequest(MailSendRequest request) {
        if (!mailProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED, "邮件服务未启用");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮件请求不能为空");
        }
        if (!StringUtils.hasText(request.getTo())) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮件收件人不能为空");
        }
        if (!StringUtils.hasText(request.getSubject())) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮件主题不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮件内容不能为空");
        }
        if (!StringUtils.hasText(resolveFrom(request))) {
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED, "邮件发件人未配置");
        }
    }

    private String resolveFrom(MailSendRequest request) {
        return StringUtils.hasText(request.getFrom()) ? request.getFrom().trim() : safeTrim(mailProperties.getFrom());
    }

    private String resolveEncoding() {
        return StringUtils.hasText(mailProperties.getDefaultEncoding())
                ? mailProperties.getDefaultEncoding().trim()
                : "UTF-8";
    }

    private String safeTrim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
