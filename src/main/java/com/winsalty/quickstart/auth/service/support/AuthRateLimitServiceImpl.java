package com.winsalty.quickstart.auth.service.support;

import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 认证限流服务实现。
 * 使用 Redis 原子自增计数并绑定固定时间窗口，避免匿名接口被高频调用。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Service
public class AuthRateLimitServiceImpl implements AuthRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitServiceImpl.class);
    private static final String LOGIN_IP_PREFIX = "sa:auth:limit:login:ip:";
    private static final String LOGIN_ACCOUNT_PREFIX = "sa:auth:limit:login:account:";
    private static final String LOGIN_FAIL_COUNT_PREFIX = "sa:auth:lock:login:fail-count:";
    private static final String LOGIN_LOCK_PREFIX = "sa:auth:lock:login:";
    private static final String FILE_UPLOAD_IP_PREFIX = "sa:auth:limit:file-upload:ip:";
    private static final String FILE_UPLOAD_USER_PREFIX = "sa:auth:limit:file-upload:user:";
    private static final String VERIFY_IP_PREFIX = "sa:auth:limit:verify:ip:";
    private static final String VERIFY_EMAIL_PREFIX = "sa:auth:limit:verify:email:";
    private static final long LOGIN_WINDOW_SECONDS = 600L;
    private static final long LOGIN_FAIL_WINDOW_SECONDS = 900L;
    private static final long LOGIN_LOCK_SECONDS = 900L;
    private static final long FILE_UPLOAD_WINDOW_SECONDS = 600L;
    private static final long VERIFY_WINDOW_SECONDS = 3600L;
    private static final long LOGIN_IP_LIMIT = 60L;
    private static final long LOGIN_ACCOUNT_LIMIT = 10L;
    private static final long LOGIN_FAIL_LIMIT = 5L;
    private static final long FILE_UPLOAD_IP_LIMIT = 60L;
    private static final long FILE_UPLOAD_USER_LIMIT = 20L;
    private static final long VERIFY_IP_LIMIT = 30L;
    private static final long VERIFY_EMAIL_LIMIT = 5L;
    private static final String UNKNOWN_KEY_PART = "unknown";
    private static final String MASKED_TARGET = "***";
    private static final String SINGLE_CHAR_MASK = "*";
    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final char EMAIL_SEPARATOR = '@';
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
    private static final int HEX_CHARS_PER_BYTE = 2;
    private static final int SINGLE_CHARACTER_LENGTH = 1;
    private static final int NEXT_HEX_INDEX_OFFSET = 1;
    private static final int BYTE_MASK = 0xFF;
    private static final int HEX_RADIX_SHIFT = 4;
    private static final int LOW_NIBBLE_MASK = 0x0F;

    private final RedisCacheService redisCacheService;

    public AuthRateLimitServiceImpl(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    /**
     * 登录入口同时按 IP 和账号维度限流，账号维度可阻断对单一用户的密码爆破。
     */
    @Override
    public void checkLogin(String username, String clientIp) {
        String normalizedUsername = normalize(username);
        String normalizedClientIp = normalize(clientIp);
        String lockKey = LOGIN_LOCK_PREFIX + digest(normalizedUsername);
        if (Boolean.TRUE.equals(redisCacheService.hasKey(lockKey))) {
            Long lockTtl = redisCacheService.ttl(lockKey);
            log.info("login blocked by temporary account lock, username={}, clientIp={}, ttlSeconds={}",
                    maskSensitiveTarget(normalizedUsername), normalizedClientIp, lockTtl);
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        checkLimit(LOGIN_IP_PREFIX + digest(normalizedClientIp), LOGIN_IP_LIMIT, LOGIN_WINDOW_SECONDS, "login-ip", normalizedClientIp);
        String accountKey = normalizedUsername + ":" + normalizedClientIp;
        checkLimit(LOGIN_ACCOUNT_PREFIX + digest(accountKey), LOGIN_ACCOUNT_LIMIT, LOGIN_WINDOW_SECONDS,
                "login-account", normalizedUsername);
    }

    /**
     * 登录失败后累计失败次数，达到阈值时对账号做短时锁定。
     */
    @Override
    public void recordLoginFailure(String username, String clientIp) {
        String normalizedUsername = normalize(username);
        String normalizedClientIp = normalize(clientIp);
        String failCountKey = LOGIN_FAIL_COUNT_PREFIX + digest(normalizedUsername);
        Long current = redisCacheService.increment(failCountKey);
        if (current != null && current == 1L) {
            redisCacheService.expire(failCountKey, LOGIN_FAIL_WINDOW_SECONDS);
        }
        if (current != null && current >= LOGIN_FAIL_LIMIT) {
            String lockKey = LOGIN_LOCK_PREFIX + digest(normalizedUsername);
            redisCacheService.set(lockKey, normalizedClientIp, LOGIN_LOCK_SECONDS);
            redisCacheService.delete(failCountKey);
            log.info("login account locked after repeated failures, username={}, clientIp={}, failCount={}, lockSeconds={}",
                    maskSensitiveTarget(normalizedUsername), normalizedClientIp, current, LOGIN_LOCK_SECONDS);
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    /**
     * 登录成功后清理账号失败状态，避免历史失败影响正常使用。
     */
    @Override
    public void recordLoginSuccess(String username, String clientIp) {
        String normalizedUsername = normalize(username);
        redisCacheService.delete(LOGIN_FAIL_COUNT_PREFIX + digest(normalizedUsername));
        redisCacheService.delete(LOGIN_LOCK_PREFIX + digest(normalizedUsername));
        log.info("login failure counter cleared, username={}, clientIp={}",
                maskSensitiveTarget(normalizedUsername), normalize(clientIp));
    }

    /**
     * 文件上传接口按用户和 IP 双维度限流，降低批量刷文件和恶意占用磁盘风险。
     */
    @Override
    public void checkFileUpload(String username, String clientIp) {
        String normalizedUsername = normalize(username);
        String normalizedClientIp = normalize(clientIp);
        checkLimit(FILE_UPLOAD_IP_PREFIX + digest(normalizedClientIp), FILE_UPLOAD_IP_LIMIT, FILE_UPLOAD_WINDOW_SECONDS,
                "file-upload-ip", normalizedClientIp);
        checkLimit(FILE_UPLOAD_USER_PREFIX + digest(normalizedUsername), FILE_UPLOAD_USER_LIMIT, FILE_UPLOAD_WINDOW_SECONDS,
                "file-upload-user", normalizedUsername);
    }

    /**
     * 注册邮箱验证邮件入口同时按 IP 和邮箱维度限流，避免单邮箱轰炸和单 IP 批量攻击。
     */
    @Override
    public void checkRegisterVerifyCode(String email, String clientIp) {
        checkLimit(VERIFY_IP_PREFIX + digest(clientIp), VERIFY_IP_LIMIT, VERIFY_WINDOW_SECONDS, "verify-ip", clientIp);
        checkLimit(VERIFY_EMAIL_PREFIX + digest(email), VERIFY_EMAIL_LIMIT, VERIFY_WINDOW_SECONDS, "verify-email", email);
    }

    private void checkLimit(String key, long limit, long windowSeconds, String scene, String target) {
        Long current = redisCacheService.increment(key);
        if (current != null && current == 1L) {
            redisCacheService.expire(key, windowSeconds);
        }
        if (current != null && current > limit) {
            log.info("auth rate limited, scene={}, target={}, current={}, limit={}, windowSeconds={}",
                    scene, maskSensitiveTarget(target), current, limit, windowSeconds);
            throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED);
        }
    }

    private String digest(String value) {
        try {
            byte[] digest = MessageDigest.getInstance(SHA_256_ALGORITHM)
                    .digest(normalize(value).getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("auth rate limit key digest failed", exception);
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : UNKNOWN_KEY_PART;
    }

    private String maskSensitiveTarget(String target) {
        String normalizedTarget = normalize(target);
        int atIndex = normalizedTarget.indexOf(EMAIL_SEPARATOR);
        if (atIndex <= 0) {
            return normalizedTarget;
        }
        String localPart = normalizedTarget.substring(0, atIndex);
        String domainPart = normalizedTarget.substring(atIndex);
        if (localPart.length() == SINGLE_CHARACTER_LENGTH) {
            return SINGLE_CHAR_MASK + domainPart;
        }
        return localPart.charAt(0) + MASKED_TARGET
                + localPart.charAt(localPart.length() - SINGLE_CHARACTER_LENGTH) + domainPart;
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
