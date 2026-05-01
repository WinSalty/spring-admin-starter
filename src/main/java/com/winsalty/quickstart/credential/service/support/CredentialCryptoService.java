package com.winsalty.quickstart.credential.service.support;

import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.credential.config.CredentialProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 凭证安全加密服务。
 * 统一提供 HMAC、AES-GCM 加解密、脱敏和摘要能力，避免明文进入日志或查询条件。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Service
public class CredentialCryptoService {

    private static final Logger log = LoggerFactory.getLogger(CredentialCryptoService.class);
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String SHA_256 = "SHA-256";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final String AES = "AES";
    private static final int AES_GCM_TAG_BITS = 128;
    private static final int AES_GCM_IV_BYTES = 12;
    private static final int AES_KEY_128_BYTES = 16;
    private static final int AES_KEY_192_BYTES = 24;
    private static final int AES_KEY_256_BYTES = 32;
    private static final int HEX_RADIX_SHIFT = 4;
    private static final int LOW_NIBBLE_MASK = 0x0F;
    private static final int BYTE_MASK = 0xFF;
    private static final int MASK_HEAD_LENGTH = 4;
    private static final int MASK_TAIL_LENGTH = 4;
    private static final int CHECKSUM_LENGTH = 8;
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private final CredentialProperties credentialProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialCryptoService(CredentialProperties credentialProperties) {
        this.credentialProperties = credentialProperties;
    }

    /**
     * 对凭证明文计算 HMAC，用于查询和去重。
     */
    public String hmacSecret(String secretText) {
        return hmac(secretText, credentialProperties.getSecretPepper(), ErrorCode.CREDENTIAL_SECRET_MISSING);
    }

    /**
     * 对提取 token 计算 HMAC，用于公开提取入口定位链接。
     */
    public String hmacToken(String token) {
        return hmac(token, credentialProperties.getExtract().getTokenSecret(), ErrorCode.CREDENTIAL_EXTRACT_SECRET_MISSING);
    }

    /**
     * 加密凭证明文。
     */
    public String encryptSecret(String secretText) {
        return encrypt(secretText, credentialProperties.getSecretEncryptionKey(), ErrorCode.CREDENTIAL_SECRET_MISSING);
    }

    /**
     * 解密凭证明文。
     */
    public String decryptSecret(String encryptedText) {
        return decrypt(encryptedText, credentialProperties.getSecretEncryptionKey(), ErrorCode.CREDENTIAL_SECRET_DECRYPT_FAILED);
    }

    /**
     * 加密提取 token。
     */
    public String encryptToken(String token) {
        return encrypt(token, credentialProperties.getExtract().getTokenEncryptionKey(), ErrorCode.CREDENTIAL_EXTRACT_SECRET_MISSING);
    }

    /**
     * 解密提取 token。
     */
    public String decryptToken(String encryptedToken) {
        return decrypt(encryptedToken, credentialProperties.getExtract().getTokenEncryptionKey(), ErrorCode.CREDENTIAL_EXTRACT_SECRET_MISSING);
    }

    /**
     * 生成短校验位。
     */
    public String checksum(String secretText) {
        String digest = sha256(secretText);
        return digest.substring(0, CHECKSUM_LENGTH).toUpperCase();
    }

    /**
     * 生成可展示脱敏值。
     */
    public String mask(String secretText) {
        String normalized = normalize(secretText);
        if (normalized.length() <= MASK_HEAD_LENGTH + MASK_TAIL_LENGTH) {
            return "****";
        }
        return normalized.substring(0, MASK_HEAD_LENGTH) + "****" + normalized.substring(normalized.length() - MASK_TAIL_LENGTH);
    }

    /**
     * SHA-256 摘要。
     */
    public String sha256(String value) {
        try {
            return toHex(MessageDigest.getInstance(SHA_256).digest(normalize(value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("credential sha256 digest failed", ex);
        }
    }

    /**
     * 判断 AES 密钥是否满足 128/192/256 bit 长度要求。
     */
    public boolean validAesKey(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        int length = key.getBytes(StandardCharsets.UTF_8).length;
        return length == AES_KEY_128_BYTES || length == AES_KEY_192_BYTES || length == AES_KEY_256_BYTES;
    }

    private String hmac(String value, String key, ErrorCode errorCode) {
        if (!StringUtils.hasText(key) || key.getBytes(StandardCharsets.UTF_8).length < AES_KEY_256_BYTES) {
            log.error("credential hmac key validation failed");
            throw new BusinessException(errorCode);
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            return toHex(mac.doFinal(normalize(value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("credential hmac failed", ex);
        }
    }

    private String encrypt(String value, String key, ErrorCode errorCode) {
        if (!validAesKey(key)) {
            log.error("credential aes key validation failed");
            throw new BusinessException(errorCode);
        }
        try {
            byte[] iv = new byte[AES_GCM_IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES),
                    new GCMParameterSpec(AES_GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(normalize(value).getBytes(StandardCharsets.UTF_8));
            byte[] raw = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, raw, 0, iv.length);
            System.arraycopy(cipherText, 0, raw, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception ex) {
            throw new BusinessException(errorCode, "凭证加密失败");
        }
    }

    private String decrypt(String encryptedValue, String key, ErrorCode errorCode) {
        if (!validAesKey(key)) {
            log.error("credential aes key validation failed");
            throw new BusinessException(errorCode);
        }
        try {
            byte[] raw = Base64.getDecoder().decode(encryptedValue);
            byte[] iv = new byte[AES_GCM_IV_BYTES];
            byte[] cipherText = new byte[raw.length - AES_GCM_IV_BYTES];
            System.arraycopy(raw, 0, iv, 0, AES_GCM_IV_BYTES);
            System.arraycopy(raw, AES_GCM_IV_BYTES, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES),
                    new GCMParameterSpec(AES_GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("credential decrypt failed, reason={}", ex.getMessage());
            throw new BusinessException(errorCode);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String toHex(byte[] bytes) {
        char[] result = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & BYTE_MASK;
            int resultIndex = index * 2;
            result[resultIndex] = HEX_DIGITS[value >>> HEX_RADIX_SHIFT];
            result[resultIndex + 1] = HEX_DIGITS[value & LOW_NIBBLE_MASK];
        }
        return new String(result);
    }
}
