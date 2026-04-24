package com.winsalty.quickstart.infra.verification;

import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import com.winsalty.quickstart.infra.mail.MailService;
import com.winsalty.quickstart.infra.mail.MailTemplateContent;
import com.winsalty.quickstart.infra.mail.MailTemplateService;
import com.winsalty.quickstart.infra.mail.StandardMailTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通用邮箱验证码服务测试。
 * 覆盖验证码摘要存储、验证状态写入、立即消费和错误次数达到阈值后的失效逻辑。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
class EmailVerificationCodeServiceImplTest {

    private static final String RAW_EMAIL = " User@Example.com ";
    private static final String NORMALIZED_EMAIL = "user@example.com";
    private static final String SCENE = "password-reset";
    private static final String HASH_SECRET = "unit-test-email-verification-secret";
    private static final String TEXT_CONTENT = "text-content";
    private static final String HTML_CONTENT = "<html>html-content</html>";
    private static final String SUBJECT = "密码重置验证码";
    private static final int HEX_CHARS_PER_BYTE = 2;
    private static final int NEXT_HEX_INDEX_OFFSET = 1;
    private static final int BYTE_MASK = 0xFF;
    private static final int HEX_RADIX_SHIFT = 4;
    private static final int LOW_NIBBLE_MASK = 0x0F;
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
    private static final String PENDING_KEY = "sa:email:verify-code:" + SCENE + ":" + fingerprint(NORMALIZED_EMAIL);
    private static final String VERIFIED_KEY = "sa:email:verified:" + SCENE + ":" + fingerprint(NORMALIZED_EMAIL);
    private static final String FAIL_KEY = "sa:email:verify-fail:" + SCENE + ":" + fingerprint(NORMALIZED_EMAIL);
    private static final long TTL_SECONDS = 300L;
    private static final long VERIFIED_TTL_SECONDS = 600L;
    private static final String VERIFIED_CACHE_VALUE = "verified";
    private static final String WRONG_CODE = "000000";
    private static final int CODE_LENGTH = 6;
    private static final long FAIL_LIMIT = 5L;

    @Test
    void sendCodeStoresHashAndSendsTemplateMail() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        MailService mailService = mock(MailService.class);
        MailTemplateService mailTemplateService = templateService();
        EmailVerificationCodeServiceImpl service = service(redisCacheService, mailService, mailTemplateService);

        service.sendCode(sendRequest());

        ArgumentCaptor<StandardMailTemplate> templateCaptor = ArgumentCaptor.forClass(StandardMailTemplate.class);
        ArgumentCaptor<Object> cacheValueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(mailTemplateService).renderStandard(templateCaptor.capture());
        verify(redisCacheService).set(eq(PENDING_KEY), cacheValueCaptor.capture(), eq(TTL_SECONDS));
        verify(redisCacheService).delete(FAIL_KEY);
        verify(redisCacheService).delete(VERIFIED_KEY);
        verify(mailService).sendHtml(eq(NORMALIZED_EMAIL), eq(SUBJECT), eq(TEXT_CONTENT), eq(HTML_CONTENT));
        String code = templateCaptor.getValue().getHighlightValue();
        assertEquals(CODE_LENGTH, code.length());
        assertTrue(code.matches("\\d{6}"));
        assertEquals("验证码", templateCaptor.getValue().getHighlightLabel());
        assertNotEquals(code, cacheValueCaptor.getValue());
    }

    @Test
    void verifyCodeWritesVerifiedStateWhenCodeMatches() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        MailService mailService = mock(MailService.class);
        MailTemplateService mailTemplateService = templateService();
        EmailVerificationCodeServiceImpl service = service(redisCacheService, mailService, mailTemplateService);
        SentCode sentCode = sendAndCapture(service, redisCacheService, mailTemplateService);
        clearInvocations(redisCacheService, mailService, mailTemplateService);
        when(redisCacheService.get(PENDING_KEY)).thenReturn(sentCode.cachedHash);

        service.verifyCode(verifyRequest(sentCode.plainCode));

        verify(redisCacheService).set(VERIFIED_KEY, VERIFIED_CACHE_VALUE, VERIFIED_TTL_SECONDS);
        verify(redisCacheService).delete(PENDING_KEY);
        verify(redisCacheService).delete(FAIL_KEY);
        verify(redisCacheService, never()).increment(FAIL_KEY);
    }

    @Test
    void consumeCodeDeletesPendingWithoutVerifiedState() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        MailService mailService = mock(MailService.class);
        MailTemplateService mailTemplateService = templateService();
        EmailVerificationCodeServiceImpl service = service(redisCacheService, mailService, mailTemplateService);
        SentCode sentCode = sendAndCapture(service, redisCacheService, mailTemplateService);
        clearInvocations(redisCacheService, mailService, mailTemplateService);
        when(redisCacheService.get(PENDING_KEY)).thenReturn(sentCode.cachedHash);

        service.consumeCode(verifyRequest(sentCode.plainCode));

        verify(redisCacheService).delete(PENDING_KEY);
        verify(redisCacheService).delete(FAIL_KEY);
        verify(redisCacheService).delete(VERIFIED_KEY);
        verify(redisCacheService, never()).set(eq(VERIFIED_KEY), eq(VERIFIED_CACHE_VALUE), eq(VERIFIED_TTL_SECONDS));
    }

    @Test
    void consumeVerifiedDeletesVerifiedState() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        MailService mailService = mock(MailService.class);
        MailTemplateService mailTemplateService = templateService();
        EmailVerificationCodeServiceImpl service = service(redisCacheService, mailService, mailTemplateService);
        when(redisCacheService.get(VERIFIED_KEY)).thenReturn(VERIFIED_CACHE_VALUE);

        service.consumeVerified(SCENE, NORMALIZED_EMAIL);

        verify(redisCacheService).delete(VERIFIED_KEY);
    }

    @Test
    void verifyCodeDeletesKeysWhenFailureLimitReached() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        MailService mailService = mock(MailService.class);
        MailTemplateService mailTemplateService = templateService();
        EmailVerificationCodeServiceImpl service = service(redisCacheService, mailService, mailTemplateService);
        SentCode sentCode = sendAndCapture(service, redisCacheService, mailTemplateService);
        clearInvocations(redisCacheService, mailService, mailTemplateService);
        when(redisCacheService.get(PENDING_KEY)).thenReturn(sentCode.cachedHash);
        when(redisCacheService.increment(FAIL_KEY)).thenReturn(FAIL_LIMIT);

        assertThrows(BusinessException.class, () -> service.verifyCode(verifyRequest(WRONG_CODE)));

        verify(redisCacheService).delete(PENDING_KEY);
        verify(redisCacheService).delete(FAIL_KEY);
    }

    private EmailVerificationCodeServiceImpl service(RedisCacheService redisCacheService,
                                                     MailService mailService,
                                                     MailTemplateService mailTemplateService) {
        return new EmailVerificationCodeServiceImpl(redisCacheService, mailService, mailTemplateService,
                new EmailVerificationCodeProperties(), HASH_SECRET);
    }

    private MailTemplateService templateService() {
        MailTemplateService mailTemplateService = mock(MailTemplateService.class);
        MailTemplateContent content = new MailTemplateContent();
        content.setTextContent(TEXT_CONTENT);
        content.setHtmlContent(HTML_CONTENT);
        when(mailTemplateService.renderStandard(any(StandardMailTemplate.class))).thenReturn(content);
        return mailTemplateService;
    }

    private EmailVerificationCodeSendRequest sendRequest() {
        EmailVerificationCodeSendRequest request = new EmailVerificationCodeSendRequest();
        request.setScene(SCENE);
        request.setEmail(RAW_EMAIL);
        request.setSubject(SUBJECT);
        request.setSummary("请使用以下验证码完成密码重置。");
        return request;
    }

    private EmailVerificationCodeVerifyRequest verifyRequest(String code) {
        EmailVerificationCodeVerifyRequest request = new EmailVerificationCodeVerifyRequest();
        request.setScene(SCENE);
        request.setEmail(NORMALIZED_EMAIL);
        request.setCode(code);
        return request;
    }

    private SentCode sendAndCapture(EmailVerificationCodeServiceImpl service,
                                    RedisCacheService redisCacheService,
                                    MailTemplateService mailTemplateService) {
        service.sendCode(sendRequest());
        ArgumentCaptor<StandardMailTemplate> templateCaptor = ArgumentCaptor.forClass(StandardMailTemplate.class);
        ArgumentCaptor<Object> cacheValueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(mailTemplateService).renderStandard(templateCaptor.capture());
        verify(redisCacheService).set(eq(PENDING_KEY), cacheValueCaptor.capture(), eq(TTL_SECONDS));
        return new SentCode(templateCaptor.getValue().getHighlightValue(), String.valueOf(cacheValueCaptor.getValue()));
    }

    private static String fingerprint(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("test fingerprint failed", exception);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] result = new char[bytes.length * HEX_CHARS_PER_BYTE];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & BYTE_MASK;
            int resultIndex = index * HEX_CHARS_PER_BYTE;
            result[resultIndex] = HEX_DIGITS[value >>> HEX_RADIX_SHIFT];
            result[resultIndex + NEXT_HEX_INDEX_OFFSET] = HEX_DIGITS[value & LOW_NIBBLE_MASK];
        }
        return new String(result);
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
