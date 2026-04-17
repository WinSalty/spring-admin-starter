package com.salty.admin.auth.service;

import com.salty.admin.common.enums.ErrorCode;
import com.salty.admin.common.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private static final int ACCOUNT_MAX_FAIL = 5;

    private static final int IP_MAX_FAIL = 20;

    private final StringRedisTemplate redisTemplate;

    public LoginAttemptService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void assertAllowed(String account, String ip) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey(account)))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号已锁定，请稍后再试");
        }
        String ipFails = redisTemplate.opsForValue().get(ipFailKey(ip));
        if (ipFails != null && Integer.parseInt(ipFails) >= IP_MAX_FAIL) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "登录失败过多，请稍后再试");
        }
    }

    public void onFailure(String account, String ip) {
        long accountFails = increment(accountFailKey(account), 30, TimeUnit.MINUTES);
        increment(ipFailKey(ip), 30, TimeUnit.MINUTES);
        if (accountFails >= ACCOUNT_MAX_FAIL) {
            redisTemplate.opsForValue().set(lockKey(account), "1", 30, TimeUnit.MINUTES);
        }
    }

    public void onSuccess(String account, String ip) {
        redisTemplate.delete(accountFailKey(account));
        redisTemplate.delete(lockKey(account));
        redisTemplate.delete(ipFailKey(ip));
    }

    private long increment(String key, long timeout, TimeUnit unit) {
        Long value = redisTemplate.opsForValue().increment(key);
        if (value != null && value == 1L) {
            redisTemplate.expire(key, timeout, unit);
        }
        return value == null ? 0L : value;
    }

    private String accountFailKey(String account) {
        return "auth:login:fail:account:" + normalize(account);
    }

    private String ipFailKey(String ip) {
        return "auth:login:fail:ip:" + (ip == null ? "unknown" : ip);
    }

    private String lockKey(String account) {
        return "auth:login:lock:account:" + normalize(account);
    }

    private String normalize(String account) {
        return account == null ? "unknown" : account.trim().toLowerCase();
    }
}
