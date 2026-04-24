package com.winsalty.quickstart.auth.service.support;

import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;

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
 * 注册邮箱验证服务测试。
 * 覆盖验证链接 token 摘要存储、链接验证成功消费和错误次数达到阈值后的失效逻辑。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
class RegisterVerificationServiceImplTest {

    private static final String RAW_EMAIL = " Test@Example.com ";
    private static final String NORMALIZED_EMAIL = "test@example.com";
    private static final String VERIFY_BASE_URL = "http://localhost:5173";
    private static final String PENDING_KEY = "sa:register:verify-link:test@example.com";
    private static final String FAIL_KEY = "sa:register:verify-fail:test@example.com";
    private static final String HASH_SECRET = "unit-test-register-verify-secret";
    private static final long LINK_TTL_SECONDS = 900L;
    private static final String WRONG_TOKEN = "wrong-token";

    @Test
    void sendVerificationLinkStoresHashInsteadOfPlainTextToken() throws Exception {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RegisterMailService registerMailService = enabledRegisterMailService();
        RegisterVerificationServiceImpl service = new RegisterVerificationServiceImpl(
                redisCacheService, registerMailService, HASH_SECRET);

        service.sendVerificationLink(RAW_EMAIL, VERIFY_BASE_URL);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> cacheValueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(registerMailService).sendVerificationLink(eq(NORMALIZED_EMAIL), urlCaptor.capture(),
                eq(LINK_TTL_SECONDS));
        verify(redisCacheService).set(eq(PENDING_KEY), cacheValueCaptor.capture(), eq(LINK_TTL_SECONDS));
        verify(redisCacheService).delete(FAIL_KEY);
        String token = extractToken(urlCaptor.getValue());
        assertTrue(urlCaptor.getValue().startsWith(VERIFY_BASE_URL + "/register/verify-email?"));
        assertTrue(urlCaptor.getValue().contains("email=test%40example.com"));
        assertTrue(token.length() > 32);
        assertNotEquals(token, cacheValueCaptor.getValue());
        assertTrue(String.valueOf(cacheValueCaptor.getValue()).length() > token.length());
    }

    @Test
    void verifyLinkDeletesPendingStateWhenTokenMatches() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RegisterMailService registerMailService = enabledRegisterMailService();
        RegisterVerificationServiceImpl service = new RegisterVerificationServiceImpl(
                redisCacheService, registerMailService, HASH_SECRET);
        SentLink sentLink = sendAndCapture(service, redisCacheService, registerMailService);
        clearInvocations(redisCacheService, registerMailService);
        when(redisCacheService.get(PENDING_KEY)).thenReturn(sentLink.cachedHash);

        service.verifyLink(NORMALIZED_EMAIL, sentLink.plainToken);

        verify(redisCacheService).delete(PENDING_KEY);
        verify(redisCacheService).delete(FAIL_KEY);
        verify(redisCacheService, never()).increment(FAIL_KEY);
    }

    @Test
    void verifyLinkDeletesKeysWhenFailureLimitReached() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RegisterMailService registerMailService = enabledRegisterMailService();
        RegisterVerificationServiceImpl service = new RegisterVerificationServiceImpl(
                redisCacheService, registerMailService, HASH_SECRET);
        SentLink sentLink = sendAndCapture(service, redisCacheService, registerMailService);
        clearInvocations(redisCacheService, registerMailService);
        when(redisCacheService.get(PENDING_KEY)).thenReturn(sentLink.cachedHash);
        when(redisCacheService.increment(FAIL_KEY)).thenReturn(5L);

        assertThrows(BusinessException.class, () -> service.verifyLink(NORMALIZED_EMAIL, WRONG_TOKEN));

        verify(redisCacheService).delete(PENDING_KEY);
        verify(redisCacheService).delete(FAIL_KEY);
    }

    @Test
    void sendVerificationLinkRejectsWhenRegisterMailDisabled() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RegisterMailService registerMailService = mock(RegisterMailService.class);
        when(registerMailService.isEnabled()).thenReturn(false);
        RegisterVerificationServiceImpl service = new RegisterVerificationServiceImpl(
                redisCacheService, registerMailService, HASH_SECRET);

        assertThrows(BusinessException.class, () -> service.sendVerificationLink(NORMALIZED_EMAIL, VERIFY_BASE_URL));

        verify(registerMailService, never()).sendVerificationLink(eq(NORMALIZED_EMAIL), anyString(),
                eq(LINK_TTL_SECONDS));
    }

    private RegisterMailService enabledRegisterMailService() {
        RegisterMailService registerMailService = mock(RegisterMailService.class);
        when(registerMailService.isEnabled()).thenReturn(true);
        return registerMailService;
    }

    private SentLink sendAndCapture(RegisterVerificationServiceImpl service,
                                    RedisCacheService redisCacheService,
                                    RegisterMailService registerMailService) {
        service.sendVerificationLink(NORMALIZED_EMAIL, VERIFY_BASE_URL);
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> cacheValueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(registerMailService).sendVerificationLink(eq(NORMALIZED_EMAIL), urlCaptor.capture(),
                eq(LINK_TTL_SECONDS));
        verify(redisCacheService).set(eq(PENDING_KEY), cacheValueCaptor.capture(), eq(LINK_TTL_SECONDS));
        return new SentLink(extractToken(urlCaptor.getValue()), String.valueOf(cacheValueCaptor.getValue()));
    }

    private String extractToken(String verificationUrl) {
        String query = URI.create(verificationUrl).getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            if (pair.startsWith("token=")) {
                return pair.substring("token=".length());
            }
        }
        throw new IllegalStateException("token missing");
    }

    private static class SentLink {

        private final String plainToken;
        private final String cachedHash;

        private SentLink(String plainToken, String cachedHash) {
            this.plainToken = plainToken;
            this.cachedHash = cachedHash;
        }
    }
}
