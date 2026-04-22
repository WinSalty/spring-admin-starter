package com.winsalty.quickstart.auth.service.support;

import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证限流服务测试。
 * 覆盖登录和验证码匿名入口的限流计数、过期窗口和超限拒绝逻辑。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
class AuthRateLimitServiceImplTest {

    @Test
    void checkLoginSetsExpireWhenCounterCreated() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        AuthRateLimitServiceImpl service = new AuthRateLimitServiceImpl(redisCacheService);
        when(redisCacheService.increment(anyString())).thenReturn(1L);

        service.checkLogin("admin", "127.0.0.1");

        verify(redisCacheService, times(2)).expire(anyString(), anyLong());
    }

    @Test
    void checkLoginThrowsWhenIpLimitExceeded() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        AuthRateLimitServiceImpl service = new AuthRateLimitServiceImpl(redisCacheService);
        when(redisCacheService.increment(anyString())).thenReturn(61L);

        assertThrows(BusinessException.class, () -> service.checkLogin("admin", "127.0.0.1"));
    }

    @Test
    void checkRegisterVerifyCodeThrowsWhenEmailLimitExceeded() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        AuthRateLimitServiceImpl service = new AuthRateLimitServiceImpl(redisCacheService);
        when(redisCacheService.increment(anyString())).thenReturn(1L, 6L);

        assertThrows(BusinessException.class, () -> service.checkRegisterVerifyCode("test@example.com", "127.0.0.1"));
    }
}
