package com.winsalty.quickstart.auth.service.support;

import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 注册邮箱验证服务实现。
 * 使用 Redis 保存邮箱激活链接 token 摘要，链接点击成功后立即消费 token。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Service
public class RegisterVerificationServiceImpl implements RegisterVerificationService {

    private static final Logger log = LoggerFactory.getLogger(RegisterVerificationServiceImpl.class);
    private static final long LINK_TTL_SECONDS = 900L;
    private static final long VERIFY_FAIL_LIMIT = 5L;
    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final String PENDING_KEY_PREFIX = "sa:register:verify-link:";
    private static final String FAIL_KEY_PREFIX = "sa:register:verify-fail:";
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final String VERIFY_TOKEN_SEPARATOR = ":";
    private static final String REGISTER_PATH = "/register";
    private static final String EMAIL_QUERY_PARAM = "email";
    private static final String TOKEN_QUERY_PARAM = "token";
    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";
    private static final char EMAIL_SEPARATOR = '@';
    private static final char URL_PATH_SEPARATOR = '/';
    private static final char URL_QUERY_PREFIX = '?';
    private static final char URL_QUERY_SEPARATOR = '&';
    private static final char URL_VALUE_SEPARATOR = '=';
    private static final String MASKED_VALUE = "***";
    private static final String SINGLE_CHAR_MASK = "*";
    private static final int SINGLE_CHARACTER_LENGTH = 1;

    private final RedisCacheService redisCacheService;
    private final RegisterMailService registerMailService;
    private final SecureRandom random = new SecureRandom();
    private final String verifyTokenHashSecret;

    public RegisterVerificationServiceImpl(RedisCacheService redisCacheService,
                                           RegisterMailService registerMailService,
                                           @Value("${app.security.jwt-secret}") String verifyTokenHashSecret) {
        this.redisCacheService = redisCacheService;
        this.registerMailService = registerMailService;
        if (!StringUtils.hasText(verifyTokenHashSecret)) {
            throw new IllegalArgumentException("register verify token hash secret must not be blank");
        }
        this.verifyTokenHashSecret = verifyTokenHashSecret;
    }

    /**
     * 生成一次性账号激活链接，邮件提交发送后缓存 token 摘要。
     */
    @Override
    public void sendVerificationLink(String email, String verifyLinkBaseUrl) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮箱不能为空");
        }
        if (!registerMailService.isEnabled()) {
            throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_SEND_FAILED, "邮箱验证服务未启用");
        }
        String normalizedEmail = normalizeEmail(email);
        String token = generateToken();
        String pendingKey = buildPendingKey(normalizedEmail);
        redisCacheService.set(pendingKey, hashVerifyToken(normalizedEmail, token), LINK_TTL_SECONDS);
        redisCacheService.delete(buildFailKey(normalizedEmail));
        try {
            registerMailService.sendVerificationLink(normalizedEmail,
                    buildVerificationUrl(verifyLinkBaseUrl, normalizedEmail, token), LINK_TTL_SECONDS);
        } catch (BusinessException exception) {
            redisCacheService.delete(pendingKey);
            throw exception;
        } catch (RuntimeException exception) {
            redisCacheService.delete(pendingKey);
            throw exception;
        }
        log.info("register activation link queued, email={}", maskEmail(normalizedEmail));
    }

    /**
     * 校验账号激活链接。验证成功后删除待验证 token，后续由认证服务激活账号。
     */
    @Override
    public void verifyLink(String email, String token) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_INVALID, "邮箱验证链接无效");
        }
        String normalizedEmail = normalizeEmail(email);
        String pendingKey = buildPendingKey(normalizedEmail);
        Object cached = redisCacheService.get(pendingKey);
        if (!(cached instanceof String) || !StringUtils.hasText((String) cached)) {
            throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_INVALID, "邮箱验证链接不存在或已过期");
        }
        String expectedHash = String.valueOf(cached);
        String actualHash = hashVerifyToken(normalizedEmail, token.trim());
        if (!constantTimeEquals(expectedHash, actualHash)) {
            recordVerifyFailure(normalizedEmail, pendingKey);
            throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_INVALID, "邮箱验证链接无效");
        }
        redisCacheService.delete(pendingKey);
        redisCacheService.delete(buildFailKey(normalizedEmail));
        log.info("register activation link verified, email={}", maskEmail(normalizedEmail));
    }

    private void recordVerifyFailure(String email, String pendingKey) {
        String failKey = buildFailKey(email);
        Long current = redisCacheService.increment(failKey);
        if (current != null && current == 1L) {
            redisCacheService.expire(failKey, LINK_TTL_SECONDS);
        }
        if (current != null && current >= VERIFY_FAIL_LIMIT) {
            redisCacheService.delete(pendingKey);
            redisCacheService.delete(failKey);
            log.info("register activation link invalidated after repeated failures, email={}, failCount={}",
                    maskEmail(email), current);
        }
    }

    private boolean constantTimeEquals(String expectedHash, String actualHash) {
        byte[] expectedBytes = expectedHash.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actualHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private String hashVerifyToken(String email, String token) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(verifyTokenHashSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256_ALGORITHM);
            mac.init(keySpec);
            byte[] digest = mac.doFinal((email + VERIFY_TOKEN_SEPARATOR + token).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("register verify token hash failed", exception);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildVerificationUrl(String verifyLinkBaseUrl, String email, String token) {
        String baseUrl = normalizeVerifyLinkBaseUrl(verifyLinkBaseUrl);
        return baseUrl + REGISTER_PATH
                + URL_QUERY_PREFIX + EMAIL_QUERY_PARAM + URL_VALUE_SEPARATOR + encodeQueryValue(email)
                + URL_QUERY_SEPARATOR + TOKEN_QUERY_PARAM + URL_VALUE_SEPARATOR + encodeQueryValue(token);
    }

    private String normalizeVerifyLinkBaseUrl(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED, "注册激活链接基础地址未配置");
        }
        String trimmed = value.trim();
        validateHttpUrl(trimmed);
        while (trimmed.charAt(trimmed.length() - SINGLE_CHARACTER_LENGTH) == URL_PATH_SEPARATOR) {
            trimmed = trimmed.substring(0, trimmed.length() - SINGLE_CHARACTER_LENGTH);
        }
        return trimmed;
    }

    private void validateHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (!HTTP_SCHEME.equalsIgnoreCase(scheme) && !HTTPS_SCHEME.equalsIgnoreCase(scheme)) {
                throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "注册激活链接协议不支持");
            }
            if (!StringUtils.hasText(uri.getHost())) {
                throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "注册激活链接格式不正确");
            }
        } catch (URISyntaxException exception) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "注册激活链接格式不正确");
        }
    }

    private String encodeQueryValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("url encode failed", exception);
        }
    }

    private String buildPendingKey(String email) {
        return PENDING_KEY_PREFIX + normalizeEmail(email);
    }

    private String buildFailKey(String email) {
        return FAIL_KEY_PREFIX + normalizeEmail(email);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return MASKED_VALUE;
        }
        int atIndex = email.indexOf(EMAIL_SEPARATOR);
        if (atIndex <= 0) {
            return MASKED_VALUE;
        }
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        if (localPart.length() == SINGLE_CHARACTER_LENGTH) {
            return SINGLE_CHAR_MASK + domainPart;
        }
        return localPart.charAt(0) + MASKED_VALUE
                + localPart.charAt(localPart.length() - SINGLE_CHARACTER_LENGTH) + domainPart;
    }
}
