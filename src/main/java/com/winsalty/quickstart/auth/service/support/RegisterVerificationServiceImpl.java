package com.winsalty.quickstart.auth.service.support;

import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Random;

/**
 * 注册验证码服务实现。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Service
public class RegisterVerificationServiceImpl implements RegisterVerificationService {

    private static final Logger log = LoggerFactory.getLogger(RegisterVerificationServiceImpl.class);
    private static final long CODE_TTL_SECONDS = 300L;
    private static final String CACHE_KEY_PREFIX = "sa:register:verify:";

    private final RedisCacheService redisCacheService;
    private final Random random = new Random();

    public RegisterVerificationServiceImpl(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    @Override
    public String generateCode(String email) {
        String code = String.format("%06d", random.nextInt(1000000));
        redisCacheService.set(buildKey(email), code, CODE_TTL_SECONDS);
        log.info("register verify code generated, email={}, code={}", email, code);
        return code;
    }

    @Override
    public void verifyCode(String email, String code) {
        Object cached = redisCacheService.get(buildKey(email));
        if (!(cached instanceof String) || !StringUtils.hasText((String) cached)) {
            throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_INVALID, "邮箱验证码不存在或已过期");
        }
        String expectedCode = String.valueOf(cached);
        if (!expectedCode.equals(code)) {
            throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_INVALID, "邮箱验证码错误");
        }
        redisCacheService.delete(buildKey(email));
    }

    private String buildKey(String email) {
        return CACHE_KEY_PREFIX + email.trim().toLowerCase();
    }
}
