package com.winsalty.quickstart.infra.mail;

import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * 基于 Spring Mail 的通用邮件发送实现。
 * 提供文本邮件和 HTML 邮件的统一异步发送能力，供各业务模块复用。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Service
public class JavaMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(JavaMailService.class);
    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final int SINGLE_ADDRESS_COUNT = 1;
    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
    private static final char EMAIL_SEPARATOR = '@';
    private static final String MASKED_VALUE = "***";
    private static final String SINGLE_CHAR_MASK = "*";
    private static final String BLANK_FINGERPRINT = "blank";
    private static final int HEX_CHARS_PER_BYTE = 2;
    private static final int SINGLE_CHARACTER_LENGTH = 1;
    private static final int NEXT_HEX_INDEX_OFFSET = 1;
    private static final int BYTE_MASK = 0xFF;
    private static final int HEX_RADIX_SHIFT = 4;
    private static final int LOW_NIBBLE_MASK = 0x0F;

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
        validateRequest(request);
        boolean html = hasHtmlContent(request);
        String maskedTo = maskEmail(request.getTo());
        String subjectFingerprint = fingerprint(request.getSubject());
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
        boolean multipart = hasHtmlContent(request) && hasTextContent(request);
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, multipart, resolveEncoding());
        helper.setFrom(parseSingleAddress(resolveFrom(), "邮件发件人格式不正确"));
        helper.setTo(parseSingleAddress(request.getTo().trim(), "邮件收件人格式不正确"));
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

    private void validateRequest(MailSendRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮件请求不能为空");
        }
        if (!mailProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED, "邮件服务未启用");
        }
        if (StringUtils.hasText(request.getFrom())) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮件发件人不允许由业务入参指定");
        }
        if (!StringUtils.hasText(request.getTo())) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮件收件人不能为空");
        }
        if (!StringUtils.hasText(request.getSubject())) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮件主题不能为空");
        }
        if (!hasTextContent(request) && !hasHtmlContent(request)) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮件内容不能为空");
        }
        if (!StringUtils.hasText(resolveFrom())) {
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED, "邮件发件人未配置");
        }
    }

    private InternetAddress parseSingleAddress(String value, String message) throws MessagingException {
        InternetAddress[] addresses = InternetAddress.parse(value, true);
        if (addresses.length != SINGLE_ADDRESS_COUNT) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, message);
        }
        addresses[0].validate();
        return addresses[0];
    }

    private String resolveFrom() {
        return safeTrim(mailProperties.getFrom());
    }

    private String resolveEncoding() {
        return StringUtils.hasText(mailProperties.getDefaultEncoding())
                ? mailProperties.getDefaultEncoding().trim()
                : DEFAULT_ENCODING;
    }

    private void applyContent(MimeMessageHelper helper, MailSendRequest request) throws MessagingException {
        if (hasTextContent(request) && hasHtmlContent(request)) {
            helper.setText(request.getTextContent().trim(), request.getHtmlContent().trim());
            return;
        }
        if (hasHtmlContent(request)) {
            helper.setText(request.getHtmlContent().trim(), true);
            return;
        }
        helper.setText(request.getTextContent().trim(), false);
    }

    private boolean hasTextContent(MailSendRequest request) {
        return StringUtils.hasText(request.getTextContent());
    }

    private boolean hasHtmlContent(MailSendRequest request) {
        return StringUtils.hasText(request.getHtmlContent());
    }

    private String safeTrim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String maskEmail(String email) {
        String value = safeTrim(email);
        if (!StringUtils.hasText(value)) {
            return MASKED_VALUE;
        }
        int atIndex = value.indexOf(EMAIL_SEPARATOR);
        if (atIndex <= 0) {
            return MASKED_VALUE;
        }
        String localPart = value.substring(0, atIndex);
        String domainPart = value.substring(atIndex);
        if (localPart.length() == SINGLE_CHARACTER_LENGTH) {
            return SINGLE_CHAR_MASK + domainPart;
        }
        return localPart.charAt(0) + MASKED_VALUE
                + localPart.charAt(localPart.length() - SINGLE_CHARACTER_LENGTH) + domainPart;
    }

    private String fingerprint(String value) {
        String normalized = safeTrim(value);
        if (!StringUtils.hasText(normalized)) {
            return BLANK_FINGERPRINT;
        }
        try {
            byte[] digest = MessageDigest.getInstance(SHA_256_ALGORITHM)
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("mail subject fingerprint failed", exception);
        }
    }

    private String toHex(byte[] bytes) {
        char[] result = new char[bytes.length * HEX_CHARS_PER_BYTE];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & BYTE_MASK;
            int resultIndex = index * HEX_CHARS_PER_BYTE;
            result[resultIndex] = HEX_DIGITS[value >>> HEX_RADIX_SHIFT];
            result[resultIndex + NEXT_HEX_INDEX_OFFSET] = HEX_DIGITS[value & LOW_NIBBLE_MASK];
        }
        return new String(result);
    }
}
