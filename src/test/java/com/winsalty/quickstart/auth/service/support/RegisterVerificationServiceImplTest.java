package com.winsalty.quickstart.auth.service.support;

import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 注册验证码服务测试。
 * 覆盖验证码摘要存储、成功消费删除和错误次数达到阈值后的失效逻辑。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
class RegisterVerificationServiceImplTest {

    private static final String RAW_EMAIL = " Test@Example.com ";
    private static final String NORMALIZED_EMAIL = "test@example.com";
    private static final String VERIFY_KEY = "sa:register:verify:test@example.com";
    private static final String FAIL_KEY = "sa:register:verify-fail:test@example.com";
    private static final String HASH_SECRET = "unit-test-register-verify-secret";
    private static final long CODE_TTL_SECONDS = 300L;
    private static final String WRONG_CODE = "000000";

    @Test
    void sendCodeStoresHashInsteadOfPlainTextCode() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RegisterMailService registerMailService = enabledRegisterMailService();
        RegisterVerificationServiceImpl service = new RegisterVerificationServiceImpl(
                redisCacheService, registerMailService, HASH_SECRET);

        service.sendCode(RAW_EMAIL);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> cacheValueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(registerMailService).sendVerifyCode(eq(NORMALIZED_EMAIL), codeCaptor.capture(), eq(CODE_TTL_SECONDS));
        verify(redisCacheService).set(eq(VERIFY_KEY), cacheValueCaptor.capture(), eq(CODE_TTL_SECONDS));
        verify(redisCacheService).delete(FAIL_KEY);
        assertTrue(codeCaptor.getValue().matches("\\d{6}"));
        assertNotEquals(codeCaptor.getValue(), cacheValueCaptor.getValue());
        assertTrue(String.valueOf(cacheValueCaptor.getValue()).length() > codeCaptor.getValue().length());
    }

    @Test
    void verifyCodeDeletesVerifyAndFailKeysWhenCodeMatches() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RegisterMailService registerMailService = enabledRegisterMailService();
        RegisterVerificationServiceImpl service = new RegisterVerificationServiceImpl(
                redisCacheService, registerMailService, HASH_SECRET);
        SentCode sentCode = sendAndCapture(service, redisCacheService, registerMailService);
        clearInvocations(redisCacheService, registerMailService);
        when(redisCacheService.get(VERIFY_KEY)).thenReturn(sentCode.cachedHash);

        service.verifyCode(NORMALIZED_EMAIL, sentCode.plainCode);

        verify(redisCacheService).delete(VERIFY_KEY);
        verify(redisCacheService).delete(FAIL_KEY);
        verify(redisCacheService, never()).increment(FAIL_KEY);
    }

    @Test
    void verifyCodeDeletesKeysWhenFailureLimitReached() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RegisterMailService registerMailService = enabledRegisterMailService();
        RegisterVerificationServiceImpl service = new RegisterVerificationServiceImpl(
                redisCacheService, registerMailService, HASH_SECRET);
        SentCode sentCode = sendAndCapture(service, redisCacheService, registerMailService);
        clearInvocations(redisCacheService, registerMailService);
        when(redisCacheService.get(VERIFY_KEY)).thenReturn(sentCode.cachedHash);
        when(redisCacheService.increment(FAIL_KEY)).thenReturn(5L);

        assertThrows(BusinessException.class, () -> service.verifyCode(NORMALIZED_EMAIL, WRONG_CODE));

        verify(redisCacheService).delete(VERIFY_KEY);
        verify(redisCacheService).delete(FAIL_KEY);
    }

    @Test
    void sendCodeRejectsWhenRegisterMailDisabled() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RegisterMailService registerMailService = mock(RegisterMailService.class);
        when(registerMailService.isEnabled()).thenReturn(false);
        RegisterVerificationServiceImpl service = new RegisterVerificationServiceImpl(
                redisCacheService, registerMailService, HASH_SECRET);

        assertThrows(BusinessException.class, () -> service.sendCode(NORMALIZED_EMAIL));

        verify(registerMailService, never()).sendVerifyCode(eq(NORMALIZED_EMAIL), anyString(), eq(CODE_TTL_SECONDS));
    }

    private RegisterMailService enabledRegisterMailService() {
        RegisterMailService registerMailService = mock(RegisterMailService.class);
        when(registerMailService.isEnabled()).thenReturn(true);
        return registerMailService;
    }

    private SentCode sendAndCapture(RegisterVerificationServiceImpl service,
                                    RedisCacheService redisCacheService,
                                    RegisterMailService registerMailService) {
        service.sendCode(NORMALIZED_EMAIL);
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> cacheValueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(registerMailService).sendVerifyCode(eq(NORMALIZED_EMAIL), codeCaptor.capture(), eq(CODE_TTL_SECONDS));
        verify(redisCacheService).set(eq(VERIFY_KEY), cacheValueCaptor.capture(), eq(CODE_TTL_SECONDS));
        return new SentCode(codeCaptor.getValue(), String.valueOf(cacheValueCaptor.getValue()));
    }

    private static class SentCode {

        private final String plainCode;
        private final String cachedHash;

        private SentCode(String plainCode, String cachedHash) {
            this.plainCode = plainCode;
            this.cachedHash = cachedHash;
        }
    }
}
