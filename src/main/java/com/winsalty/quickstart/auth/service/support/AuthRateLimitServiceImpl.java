package com.winsalty.quickstart.auth.service.support;

import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

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
    private static final String VERIFY_IP_PREFIX = "sa:auth:limit:verify:ip:";
    private static final String VERIFY_EMAIL_PREFIX = "sa:auth:limit:verify:email:";
    private static final long LOGIN_WINDOW_SECONDS = 600L;
    private static final long VERIFY_WINDOW_SECONDS = 3600L;
    private static final long LOGIN_IP_LIMIT = 60L;
    private static final long LOGIN_ACCOUNT_LIMIT = 10L;
    private static final long VERIFY_IP_LIMIT = 30L;
    private static final long VERIFY_EMAIL_LIMIT = 5L;
    private static final String UNKNOWN_KEY_PART = "unknown";

    private final RedisCacheService redisCacheService;

    public AuthRateLimitServiceImpl(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    /**
     * 登录入口同时按 IP 和账号维度限流，账号维度可阻断对单一用户的密码爆破。
     */
    @Override
    public void checkLogin(String username, String clientIp) {
        checkLimit(LOGIN_IP_PREFIX + digest(clientIp), LOGIN_IP_LIMIT, LOGIN_WINDOW_SECONDS, "login-ip", clientIp);
        String accountKey = normalize(username) + ":" + normalize(clientIp);
        checkLimit(LOGIN_ACCOUNT_PREFIX + digest(accountKey), LOGIN_ACCOUNT_LIMIT, LOGIN_WINDOW_SECONDS, "login-account", username);
    }

    /**
     * 验证码入口同时按 IP 和邮箱维度限流，避免单邮箱轰炸和单 IP 批量攻击。
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
                    scene, target, current, limit, windowSeconds);
            throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED);
        }
    }

    private String digest(String value) {
        return DigestUtils.md5DigestAsHex(normalize(value).getBytes(StandardCharsets.UTF_8));
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : UNKNOWN_KEY_PART;
    }
}
