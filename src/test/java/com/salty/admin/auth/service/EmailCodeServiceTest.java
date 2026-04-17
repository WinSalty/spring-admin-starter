package com.salty.admin.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.salty.admin.auth.entity.SysEmailVerifyCode;
import com.salty.admin.auth.mapper.SysEmailVerifyCodeMapper;
import com.salty.admin.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailCodeServiceTest {

    private SysEmailVerifyCodeMapper codeMapper;

    private StringRedisTemplate redisTemplate;

    private EmailCodeService service;

    private final HashService hashService = new HashService();

    @BeforeEach
    void setUp() {
        codeMapper = mock(SysEmailVerifyCodeMapper.class);
        redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        service = new EmailCodeService(codeMapper, redisTemplate, hashService);
        ReflectionTestUtils.setField(service, "random", new FixedSecureRandom(123456));
    }

    @Test
    void sendRegisterCodeStoresHashOnlyAndFrequencyKeys() {
        service.sendRegisterCode(" USER@Example.COM ", "127.0.0.1");

        ArgumentCaptor<SysEmailVerifyCode> captor = ArgumentCaptor.forClass(SysEmailVerifyCode.class);
        verify(codeMapper).insert(captor.capture());
        SysEmailVerifyCode record = captor.getValue();
        assertEquals("user@example.com", record.getEmail());
        assertEquals("register", record.getScene());
        assertEquals(hashService.sha256("user@example.com:register:123456"), record.getCodeHash());
        assertNotEquals("123456", record.getCodeHash());
        assertEquals(0, record.getStatus());
        assertNotNull(record.getExpireTime());
        verify(redisTemplate.opsForValue()).set("auth:email-code:freq:email:user@example.com", "1", 60, java.util.concurrent.TimeUnit.SECONDS);
        verify(redisTemplate.opsForValue()).set("auth:email-code:freq:ip:127.0.0.1", "1", 30, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Test
    void consumeRegisterCodeMarksValidCodeAsUsed() {
        SysEmailVerifyCode record = new SysEmailVerifyCode();
        record.setEmail("user@example.com");
        record.setScene("register");
        record.setStatus(0);
        record.setCodeHash(hashService.sha256("user@example.com:register:123456"));
        record.setExpireTime(LocalDateTime.now().plusMinutes(5));
        when(codeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

        service.consumeRegisterCode("USER@example.com", "123456");

        assertEquals(1, record.getStatus());
        assertNotNull(record.getUsedTime());
        verify(codeMapper).updateById(record);
    }

    @Test
    void consumeRegisterCodeRejectsWrongCode() {
        SysEmailVerifyCode record = new SysEmailVerifyCode();
        record.setEmail("user@example.com");
        record.setScene("register");
        record.setStatus(0);
        record.setCodeHash(hashService.sha256("user@example.com:register:123456"));
        record.setExpireTime(LocalDateTime.now().plusMinutes(5));
        when(codeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

        assertThrows(BusinessException.class, () -> service.consumeRegisterCode("user@example.com", "654321"));
    }

    private static class FixedSecureRandom extends SecureRandom {

        private final int value;

        private FixedSecureRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return value;
        }
    }
}
