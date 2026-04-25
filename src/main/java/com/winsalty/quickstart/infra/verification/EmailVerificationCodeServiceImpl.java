package com.winsalty.quickstart.infra.verification;

import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import com.winsalty.quickstart.infra.mail.MailService;
import com.winsalty.quickstart.infra.mail.MailTemplateContent;
import com.winsalty.quickstart.infra.mail.MailTemplateService;
import com.winsalty.quickstart.infra.mail.StandardMailTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 通用邮箱验证码服务实现。
 * 使用 Redis 按业务场景隔离验证码，缓存 HMAC 摘要而非明文验证码，并限制错误次数。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Service
public class EmailVerificationCodeServiceImpl implements EmailVerificationCodeService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationCodeServiceImpl.class);
    private static final String PENDING_KEY_PREFIX = "sa:email:verify-code:";
    private static final String VERIFIED_KEY_PREFIX = "sa:email:verified:";
    private static final String FAIL_KEY_PREFIX = "sa:email:verify-fail:";
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final String DIGEST_SEPARATOR = ":";
    private static final String VERIFIED_CACHE_VALUE = "verified";
    private static final String DEFAULT_SUBJECT = "邮箱验证码";
    private static final String DEFAULT_TITLE = "邮箱验证码";
    private static final String DEFAULT_GREETING = "您好，";
    private static final String DEFAULT_SUMMARY = "请使用以下验证码完成当前操作。";
    private static final String DEFAULT_HIGHLIGHT_LABEL = "验证码";
    private static final String DEFAULT_FOOTER_NOTE = "此邮件由系统自动发送，请勿直接回复。";
    private static final String TTL_DESCRIPTION_PREFIX = "验证码有效期为 ";
    private static final String TTL_DESCRIPTION_SUFFIX = "，请勿泄露给他人。如非本人操作，可以忽略本邮件。";
    private static final String MINUTE_UNIT_TEXT = " 分钟";
    private static final String SECOND_UNIT_TEXT = " 秒";
    private static final String MASKED_VALUE = "***";
    private static final String SINGLE_CHAR_MASK = "*";
    private static final char EMAIL_SEPARATOR = '@';
    private static final char[] CODE_DIGITS = "0123456789".toCharArray();
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
    private static final Pattern SCENE_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]*$");
    private static final int MIN_CODE_LENGTH = 4;
    private static final int MAX_CODE_LENGTH = 8;
    private static final int MIN_SCENE_LENGTH = 2;
    private static final int MAX_SCENE_LENGTH = 64;
    private static final int SINGLE_ADDRESS_COUNT = 1;
    private static final int SINGLE_CHARACTER_LENGTH = 1;
    private static final int HEX_CHARS_PER_BYTE = 2;
    private static final int NEXT_HEX_INDEX_OFFSET = 1;
    private static final int BYTE_MASK = 0xFF;
    private static final int HEX_RADIX_SHIFT = 4;
    private static final int LOW_NIBBLE_MASK = 0x0F;
    private static final long MIN_TTL_SECONDS = 1L;
    private static final long MIN_FAIL_LIMIT = 1L;
    private static final long FIRST_FAILURE_COUNT = 1L;
    private static final long ZERO_SECONDS = 0L;

    private final RedisCacheService redisCacheService;
    private final MailService mailService;
    private final MailTemplateService mailTemplateService;
    private final EmailVerificationCodeProperties properties;
    private final SecureRandom random = new SecureRandom();
    private final String hashSecret;

    public EmailVerificationCodeServiceImpl(RedisCacheService redisCacheService,
                                            MailService mailService,
                                            MailTemplateService mailTemplateService,
                                            EmailVerificationCodeProperties properties,
                                            @Value("${app.security.jwt-secret}") String hashSecret) {
        this.redisCacheService = redisCacheService;
        this.mailService = mailService;
        this.mailTemplateService = mailTemplateService;
        this.properties = properties;
        if (!StringUtils.hasText(hashSecret)) {
            throw new IllegalArgumentException("email verification code hash secret must not be blank");
        }
        this.hashSecret = hashSecret;
    }

    /**
     * 生成数字验证码并发送统一样式邮件，Redis 只保存验证码摘要。
     */
    @Override
    public void sendCode(EmailVerificationCodeSendRequest request) {
        if (!properties.isEnabled()) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFY_CODE_SEND_FAILED, "通用邮箱验证码服务未启用");
        }
        String scene = normalizeScene(request == null ? null : request.getScene());
        String email = normalizeEmail(request == null ? null : request.getEmail());
        long ttlSeconds = resolveTtlSeconds();
        String code = generateCode(resolveCodeLength());
        String pendingKey = buildPendingKey(scene, email);
        // Redis 只保存 HMAC 摘要，验证码明文只存在于本次邮件内容中，降低缓存泄露风险。
        redisCacheService.set(pendingKey, hashCode(scene, email, code), ttlSeconds);
        // 重新发送验证码时清理失败次数和已验证状态，避免旧状态影响新一轮验证。
        redisCacheService.delete(buildFailKey(scene, email));
        redisCacheService.delete(buildVerifiedKey(scene, email));
        try {
            MailTemplateContent content = mailTemplateService.renderStandard(buildTemplate(request, code, ttlSeconds));
            mailService.sendHtml(email, resolveSubject(request), content.getTextContent(), content.getHtmlContent());
        } catch (RuntimeException exception) {
            // 邮件未成功入队时同步删除验证码，避免用户收到失败提示但缓存中仍有可用验证码。
            redisCacheService.delete(pendingKey);
            throw exception;
        }
        log.info("email verification code queued, scene={}, email={}", scene, maskEmail(email));
    }

    /**
     * 校验验证码并写入已验证状态，业务提交动作需调用 consumeVerified 完成一次性消费。
     */
    @Override
    public void verifyCode(EmailVerificationCodeVerifyRequest request) {
        VerificationContext context = validatePendingCode(request);
        redisCacheService.set(buildVerifiedKey(context.getScene(), context.getEmail()),
                VERIFIED_CACHE_VALUE, resolveVerifiedTtlSeconds());
        // 验证通过后删除 pending code，后续业务动作只能消费 verified 状态，验证码本身不可重复使用。
        redisCacheService.delete(context.getPendingKey());
        redisCacheService.delete(context.getFailKey());
        log.info("email verification code verified, scene={}, email={}",
                context.getScene(), maskEmail(context.getEmail()));
    }

    /**
     * 校验验证码并立即删除验证码状态，适合单请求完成验证和业务动作的场景。
     */
    @Override
    public void consumeCode(EmailVerificationCodeVerifyRequest request) {
        VerificationContext context = validatePendingCode(request);
        redisCacheService.delete(context.getPendingKey());
        redisCacheService.delete(context.getFailKey());
        redisCacheService.delete(buildVerifiedKey(context.getScene(), context.getEmail()));
        log.info("email verification code consumed, scene={}, email={}",
                context.getScene(), maskEmail(context.getEmail()));
    }

    /**
     * 一次性消费已验证状态，验证过期或重复消费都会按验证码失效处理。
     */
    @Override
    public void consumeVerified(String scene, String email) {
        String normalizedScene = normalizeScene(scene);
        String normalizedEmail = normalizeEmail(email);
        String verifiedKey = buildVerifiedKey(normalizedScene, normalizedEmail);
        Object cached = redisCacheService.get(verifiedKey);
        if (!VERIFIED_CACHE_VALUE.equals(cached)) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFY_CODE_INVALID, "邮箱验证码未验证或已过期");
        }
        // verified 状态一次性消费，防止同一验证码验证结果被多个业务请求复用。
        redisCacheService.delete(verifiedKey);
        log.info("email verification status consumed, scene={}, email={}",
                normalizedScene, maskEmail(normalizedEmail));
    }

    private VerificationContext validatePendingCode(EmailVerificationCodeVerifyRequest request) {
        String scene = normalizeScene(request == null ? null : request.getScene());
        String email = normalizeEmail(request == null ? null : request.getEmail());
        String code = normalizeCode(request == null ? null : request.getCode());
        String pendingKey = buildPendingKey(scene, email);
        String failKey = buildFailKey(scene, email);
        Object cached = redisCacheService.get(pendingKey);
        if (!(cached instanceof String) || !StringUtils.hasText((String) cached)) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFY_CODE_INVALID, "邮箱验证码不存在或已过期");
        }
        String actualHash = hashCode(scene, email, code);
        // 使用常量时间比较，避免通过响应耗时推测验证码摘要前缀。
        if (!constantTimeEquals(String.valueOf(cached), actualHash)) {
            recordVerifyFailure(scene, email, pendingKey, failKey);
            throw new BusinessException(ErrorCode.EMAIL_VERIFY_CODE_INVALID, "邮箱验证码无效");
        }
        return new VerificationContext(scene, email, pendingKey, failKey);
    }

    private void recordVerifyFailure(String scene, String email, String pendingKey, String failKey) {
        Long current = redisCacheService.increment(failKey);
        if (current != null && current == FIRST_FAILURE_COUNT) {
            // 失败次数与验证码同生命周期，验证码过期后失败计数自动失效。
            redisCacheService.expire(failKey, resolveTtlSeconds());
        }
        if (current != null && current >= resolveFailLimit()) {
            // 连续错误达到阈值后立即删除验证码，缩小暴力枚举窗口。
            redisCacheService.delete(pendingKey);
            redisCacheService.delete(failKey);
            log.info("email verification code invalidated after repeated failures, scene={}, email={}, failCount={}",
                    scene, maskEmail(email), current);
        }
    }

    private StandardMailTemplate buildTemplate(EmailVerificationCodeSendRequest request, String code, long ttlSeconds) {
        StandardMailTemplate template = new StandardMailTemplate();
        template.setTitle(resolveText(request == null ? null : request.getTitle(),
                resolveText(properties.getTitle(), DEFAULT_TITLE)));
        template.setGreeting(resolveText(request == null ? null : request.getGreeting(), DEFAULT_GREETING));
        template.setSummary(resolveText(request == null ? null : request.getSummary(), DEFAULT_SUMMARY));
        template.setHighlightLabel(DEFAULT_HIGHLIGHT_LABEL);
        template.setHighlightValue(code);
        template.setDescription(resolveText(request == null ? null : request.getDescription(),
                TTL_DESCRIPTION_PREFIX + formatTtlText(ttlSeconds) + TTL_DESCRIPTION_SUFFIX));
        template.setFooterNote(resolveText(request == null ? null : request.getFooterNote(), DEFAULT_FOOTER_NOTE));
        return template;
    }

    private String resolveSubject(EmailVerificationCodeSendRequest request) {
        return resolveText(request == null ? null : request.getSubject(),
                resolveText(properties.getSubject(), DEFAULT_SUBJECT));
    }

    private int resolveCodeLength() {
        int codeLength = properties.getCodeLength();
        if (codeLength < MIN_CODE_LENGTH || codeLength > MAX_CODE_LENGTH) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮箱验证码长度配置不合法");
        }
        return codeLength;
    }

    private long resolveTtlSeconds() {
        long ttlSeconds = properties.getTtlSeconds();
        if (ttlSeconds < MIN_TTL_SECONDS) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮箱验证码有效期配置不合法");
        }
        return ttlSeconds;
    }

    private long resolveVerifiedTtlSeconds() {
        long verifiedTtlSeconds = properties.getVerifiedTtlSeconds();
        if (verifiedTtlSeconds < MIN_TTL_SECONDS) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮箱验证码已验证状态有效期配置不合法");
        }
        return verifiedTtlSeconds;
    }

    private long resolveFailLimit() {
        long failLimit = properties.getFailLimit();
        if (failLimit < MIN_FAIL_LIMIT) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮箱验证码失败次数限制配置不合法");
        }
        return failLimit;
    }

    private String generateCode(int codeLength) {
        StringBuilder builder = new StringBuilder(codeLength);
        for (int index = 0; index < codeLength; index++) {
            builder.append(CODE_DIGITS[random.nextInt(CODE_DIGITS.length)]);
        }
        return builder.toString();
    }

    private String hashCode(String scene, String email, String code) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256_ALGORITHM);
            mac.init(keySpec);
            // 摘要绑定 scene 和 email，避免不同业务场景或邮箱之间复用同一验证码。
            byte[] digest = mac.doFinal((scene + DIGEST_SEPARATOR + email + DIGEST_SEPARATOR + code)
                    .getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("email verification code hash failed", exception);
        }
    }

    private boolean constantTimeEquals(String expectedHash, String actualHash) {
        byte[] expectedBytes = expectedHash.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actualHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private String buildPendingKey(String scene, String email) {
        // Redis key 使用邮箱指纹而非明文邮箱，避免运维侧直接暴露用户地址。
        return PENDING_KEY_PREFIX + scene + DIGEST_SEPARATOR + fingerprint(email);
    }

    private String buildVerifiedKey(String scene, String email) {
        return VERIFIED_KEY_PREFIX + scene + DIGEST_SEPARATOR + fingerprint(email);
    }

    private String buildFailKey(String scene, String email) {
        return FAIL_KEY_PREFIX + scene + DIGEST_SEPARATOR + fingerprint(email);
    }

    private String normalizeScene(String scene) {
        if (!StringUtils.hasText(scene)) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "验证码场景编码不能为空");
        }
        String normalized = scene.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < MIN_SCENE_LENGTH || normalized.length() > MAX_SCENE_LENGTH
                || !SCENE_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID,
                    "验证码场景编码只允许字母、数字、点、下划线和中划线");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮箱不能为空");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        try {
            InternetAddress[] addresses = InternetAddress.parse(normalized, true);
            if (addresses.length != SINGLE_ADDRESS_COUNT) {
                throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮箱格式不正确");
            }
            addresses[0].validate();
            if (!normalized.equals(addresses[0].getAddress())) {
                throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮箱格式不正确");
            }
            return normalized;
        } catch (AddressException exception) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "邮箱格式不正确");
        }
    }

    private String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFY_CODE_INVALID, "邮箱验证码不能为空");
        }
        return code.trim();
    }

    private String resolveText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String formatTtlText(long ttlSeconds) {
        long minutes = TimeUnit.SECONDS.toMinutes(ttlSeconds);
        if (minutes > ZERO_SECONDS) {
            return minutes + MINUTE_UNIT_TEXT;
        }
        return ttlSeconds + SECOND_UNIT_TEXT;
    }

    private String fingerprint(String value) {
        try {
            byte[] digest = MessageDigest.getInstance(SHA_256_ALGORITHM)
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("email verification code fingerprint failed", exception);
        }
    }

    private String toHex(byte[] bytes) {
        char[] result = new char[bytes.length * HEX_CHARS_PER_BYTE];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & BYTE_MASK;
            int resultIndex = index * HEX_CHARS_PER_BYTE;
            result[resultIndex] = HEX_DIGITS[value >>> HEX_RADIX_SHIFT];
            result[resultIndex + NEXT_HEX_INDEX_OFFSET] = HEX_DIGITS[value & LOW_NIBBLE_MASK];
        }
        return new String(result);
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return MASKED_VALUE;
        }
        int atIndex = email.indexOf(EMAIL_SEPARATOR);
        if (atIndex <= 0) {
            return MASKED_VALUE;
        }
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        if (localPart.length() == SINGLE_CHARACTER_LENGTH) {
            return SINGLE_CHAR_MASK + domainPart;
        }
        return localPart.charAt(0) + MASKED_VALUE
                + localPart.charAt(localPart.length() - SINGLE_CHARACTER_LENGTH) + domainPart;
    }

    /**
     * 验证码校验上下文。
     * 缓存规范化后的场景、邮箱和 Redis key，避免成功后重复构造。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private static class VerificationContext {

        private final String scene;
        private final String email;
        private final String pendingKey;
        private final String failKey;

        private VerificationContext(String scene, String email, String pendingKey, String failKey) {
            this.scene = scene;
            this.email = email;
            this.pendingKey = pendingKey;
            this.failKey = failKey;
        }

        private String getScene() {
            return scene;
        }

        private String getEmail() {
            return email;
        }

        private String getPendingKey() {
            return pendingKey;
        }

        private String getFailKey() {
            return failKey;
        }
    }
}
