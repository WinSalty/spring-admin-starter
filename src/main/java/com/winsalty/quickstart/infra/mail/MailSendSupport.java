package com.winsalty.quickstart.infra.mail;

import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 邮件发送通用校验与脱敏工具。
 * 统一收敛 SMTP 和阿里云邮件发送实现共用的参数校验、地址解析和日志脱敏逻辑。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
final class MailSendSupport {

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

    private MailSendSupport() {
    }

    static void validateRequest(MailProperties mailProperties, MailSendRequest request) {
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
    }

    static InternetAddress parseSingleAddress(String value, String message) throws MessagingException {
        InternetAddress[] addresses = InternetAddress.parse(value, true);
        if (addresses.length != SINGLE_ADDRESS_COUNT) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, message);
        }
        addresses[0].validate();
        return addresses[0];
    }

    static String resolveFrom(MailProperties mailProperties) {
        return safeTrim(mailProperties.getFrom());
    }

    static String resolveEncoding(MailProperties mailProperties) {
        return StringUtils.hasText(mailProperties.getDefaultEncoding())
                ? mailProperties.getDefaultEncoding().trim()
                : DEFAULT_ENCODING;
    }

    static boolean hasTextContent(MailSendRequest request) {
        return StringUtils.hasText(request.getTextContent());
    }

    static boolean hasHtmlContent(MailSendRequest request) {
        return StringUtils.hasText(request.getHtmlContent());
    }

    static String safeTrim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    static String maskEmail(String email) {
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

    static String fingerprint(String value) {
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

    private static String toHex(byte[] bytes) {
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
