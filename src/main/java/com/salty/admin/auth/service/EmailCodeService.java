package com.salty.admin.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.salty.admin.auth.entity.SysEmailVerifyCode;
import com.salty.admin.auth.mapper.SysEmailVerifyCodeMapper;
import com.salty.admin.common.enums.ErrorCode;
import com.salty.admin.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class EmailCodeService {

    private static final Logger log = LoggerFactory.getLogger(EmailCodeService.class);

    private static final String SCENE_REGISTER = "register";

    private final SecureRandom random = new SecureRandom();

    private final SysEmailVerifyCodeMapper codeMapper;

    private final StringRedisTemplate redisTemplate;

    private final HashService hashService;

    public EmailCodeService(SysEmailVerifyCodeMapper codeMapper, StringRedisTemplate redisTemplate, HashService hashService) {
        this.codeMapper = codeMapper;
        this.redisTemplate = redisTemplate;
        this.hashService = hashService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void sendRegisterCode(String email, String ip) {
        String normalizedEmail = normalizeEmail(email);
        ensureSendAllowed(normalizedEmail, ip);
        String code = String.format("%06d", random.nextInt(1000000));

        SysEmailVerifyCode record = new SysEmailVerifyCode();
        record.setEmail(normalizedEmail);
        record.setScene(SCENE_REGISTER);
        record.setCodeHash(hashService.sha256(normalizedEmail + ":" + SCENE_REGISTER + ":" + code));
        record.setStatus(0);
        record.setExpireTime(LocalDateTime.now().plusMinutes(10));
        record.setRequestIp(ip);
        record.setDeleted(0);
        codeMapper.insert(record);

        redisTemplate.opsForValue().set(emailKey(normalizedEmail), "1", 60, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(ipKey(ip), "1", 30, TimeUnit.SECONDS);
        log.info("Email verification code sent, email={}, scene={}", normalizedEmail, SCENE_REGISTER);
    }

    @Transactional(rollbackFor = Exception.class)
    public void consumeRegisterCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        SysEmailVerifyCode record = codeMapper.selectOne(new LambdaQueryWrapper<SysEmailVerifyCode>()
                .eq(SysEmailVerifyCode::getEmail, normalizedEmail)
                .eq(SysEmailVerifyCode::getScene, SCENE_REGISTER)
                .eq(SysEmailVerifyCode::getStatus, 0)
                .orderByDesc(SysEmailVerifyCode::getCreateTime)
                .last("LIMIT 1"));
        if (record == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "验证码无效");
        }
        LocalDateTime now = LocalDateTime.now();
        if (record.getExpireTime().isBefore(now)) {
            record.setStatus(2);
            codeMapper.updateById(record);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "验证码已过期");
        }
        String expected = hashService.sha256(normalizedEmail + ":" + SCENE_REGISTER + ":" + code);
        if (!expected.equals(record.getCodeHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "验证码错误");
        }
        record.setStatus(1);
        record.setUsedTime(now);
        codeMapper.updateById(record);
    }

    public String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private void ensureSendAllowed(String email, String ip) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(emailKey(email)))) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "邮箱验证码发送过于频繁");
        }
        if (ip != null && Boolean.TRUE.equals(redisTemplate.hasKey(ipKey(ip)))) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "验证码请求过于频繁");
        }
    }

    private String emailKey(String email) {
        return "auth:email-code:freq:email:" + email;
    }

    private String ipKey(String ip) {
        return "auth:email-code:freq:ip:" + (ip == null ? "unknown" : ip);
    }
}
