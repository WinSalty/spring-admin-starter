package com.winsalty.quickstart.auth.service.support;

import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;

/**
 * 注册验证码服务实现。
 * 使用 Redis 保存邮箱验证码，验证码验证成功后立即删除，避免重复使用。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Service
public class RegisterVerificationServiceImpl implements RegisterVerificationService {

    private static final Logger log = LoggerFactory.getLogger(RegisterVerificationServiceImpl.class);
    private static final long CODE_TTL_SECONDS = 300L;
    private static final String CACHE_KEY_PREFIX = "sa:register:verify:";

    private final RedisCacheService redisCacheService;
    private final RegisterMailService registerMailService;
    private final SecureRandom random = new SecureRandom();

    public RegisterVerificationServiceImpl(RedisCacheService redisCacheService,
                                           RegisterMailService registerMailService) {
        this.redisCacheService = redisCacheService;
        this.registerMailService = registerMailService;
    }

    /**
     * 生成 6 位数字验证码，邮件发送成功后再缓存 5 分钟。
     */
    @Override
    public void sendCode(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮箱不能为空");
        }
        // SecureRandom 用于验证码，避免普通 Random 在高并发注册场景下可预测。
        String code = String.format("%06d", random.nextInt(1000000));
        // 先发邮件再缓存，避免邮件发送失败但 Redis 中留下一个用户永远收不到的验证码。
        registerMailService.sendVerifyCode(email.trim(), code, CODE_TTL_SECONDS);
        redisCacheService.set(buildKey(email), code, CODE_TTL_SECONDS);
        log.info("register verify code sent, email={}", email);
    }

    /**
     * 校验验证码并删除缓存。验证码不存在、过期或不匹配都按同一业务码返回。
     */
    @Override
    public void verifyCode(String email, String code) {
        Object cached = redisCacheService.get(buildKey(email));
        if (!(cached instanceof String) || !StringUtils.hasText((String) cached)) {
            throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_INVALID, "邮箱验证码不存在或已过期");
        }
        String expectedCode = String.valueOf(cached);
        if (!expectedCode.equals(code)) {
            // 错误验证码不删除缓存，允许用户在有效期内重新输入正确验证码。
            throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_INVALID, "邮箱验证码错误");
        }
        // 验证码一次性消费，校验成功立即删除，防止同一邮箱验证码被重复注册。
        redisCacheService.delete(buildKey(email));
    }

    private String buildKey(String email) {
        // 邮箱统一小写去空格，避免 Test@A.com 与 test@a.com 命中不同验证码。
        return CACHE_KEY_PREFIX + email.trim().toLowerCase();
    }
}
