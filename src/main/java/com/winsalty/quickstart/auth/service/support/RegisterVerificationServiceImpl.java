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
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 注册验证码服务实现。
 * 使用 Redis 保存邮箱验证码摘要，验证码验证成功后立即删除，避免重复使用。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Service
public class RegisterVerificationServiceImpl implements RegisterVerificationService {

    private static final Logger log = LoggerFactory.getLogger(RegisterVerificationServiceImpl.class);
    private static final long CODE_TTL_SECONDS = 300L;
    private static final long VERIFY_FAIL_LIMIT = 5L;
    private static final int CODE_RANDOM_BOUND = 1000000;
    private static final String CODE_FORMAT = "%06d";
    private static final String CACHE_KEY_PREFIX = "sa:register:verify:";
    private static final String FAIL_KEY_PREFIX = "sa:register:verify-fail:";
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final String VERIFY_CODE_SEPARATOR = ":";
    private static final char EMAIL_SEPARATOR = '@';
    private static final String MASKED_VALUE = "***";
    private static final String SINGLE_CHAR_MASK = "*";
    private static final int SINGLE_CHARACTER_LENGTH = 1;

    private final RedisCacheService redisCacheService;
    private final RegisterMailService registerMailService;
    private final SecureRandom random = new SecureRandom();
    private final String verifyCodeHashSecret;

    public RegisterVerificationServiceImpl(RedisCacheService redisCacheService,
                                           RegisterMailService registerMailService,
                                           @Value("${app.security.jwt-secret}") String verifyCodeHashSecret) {
        this.redisCacheService = redisCacheService;
        this.registerMailService = registerMailService;
        if (!StringUtils.hasText(verifyCodeHashSecret)) {
            throw new IllegalArgumentException("register verify code hash secret must not be blank");
        }
        this.verifyCodeHashSecret = verifyCodeHashSecret;
    }

    /**
     * 生成 6 位数字验证码，邮件提交发送后再缓存 5 分钟。
     */
    @Override
    public void sendCode(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮箱不能为空");
        }
        if (!registerMailService.isEnabled()) {
            throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_SEND_FAILED, "邮箱验证码服务未启用");
        }
        String normalizedEmail = normalizeEmail(email);
        // SecureRandom 用于验证码，避免普通 Random 在高并发注册场景下可预测。
        String code = String.format(CODE_FORMAT, random.nextInt(CODE_RANDOM_BOUND));
        registerMailService.sendVerifyCode(normalizedEmail, code, CODE_TTL_SECONDS);
        redisCacheService.set(buildKey(normalizedEmail), hashVerifyCode(normalizedEmail, code), CODE_TTL_SECONDS);
        redisCacheService.delete(buildFailKey(normalizedEmail));
        log.info("register verify code queued, email={}", maskEmail(normalizedEmail));
    }

    /**
     * 校验验证码并删除缓存。验证码不存在、过期、错误或错误次数过多都按同一业务码返回。
     */
    @Override
    public void verifyCode(String email, String code) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_INVALID, "邮箱验证码无效");
        }
        String normalizedEmail = normalizeEmail(email);
        String verifyKey = buildKey(normalizedEmail);
        Object cached = redisCacheService.get(verifyKey);
        if (!(cached instanceof String) || !StringUtils.hasText((String) cached)) {
            throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_INVALID, "邮箱验证码不存在或已过期");
        }
        String expectedHash = String.valueOf(cached);
        String actualHash = hashVerifyCode(normalizedEmail, code.trim());
        if (!constantTimeEquals(expectedHash, actualHash)) {
            recordVerifyFailure(normalizedEmail, verifyKey);
            throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_INVALID, "邮箱验证码错误");
        }
        redisCacheService.delete(verifyKey);
        redisCacheService.delete(buildFailKey(normalizedEmail));
    }

    private void recordVerifyFailure(String email, String verifyKey) {
        String failKey = buildFailKey(email);
        Long current = redisCacheService.increment(failKey);
        if (current != null && current == 1L) {
            redisCacheService.expire(failKey, CODE_TTL_SECONDS);
        }
        if (current != null && current >= VERIFY_FAIL_LIMIT) {
            redisCacheService.delete(verifyKey);
            redisCacheService.delete(failKey);
            log.info("register verify code invalidated after repeated failures, email={}, failCount={}", maskEmail(email), current);
        }
    }

    private boolean constantTimeEquals(String expectedHash, String actualHash) {
        byte[] expectedBytes = expectedHash.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actualHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private String hashVerifyCode(String email, String code) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(verifyCodeHashSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
            mac.init(keySpec);
            byte[] digest = mac.doFinal((email + VERIFY_CODE_SEPARATOR + code).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("register verify code hash failed", exception);
        }
    }

    private String buildKey(String email) {
        return CACHE_KEY_PREFIX + normalizeEmail(email);
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
