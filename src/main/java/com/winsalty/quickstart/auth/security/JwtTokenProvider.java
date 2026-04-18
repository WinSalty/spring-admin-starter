package com.winsalty.quickstart.auth.security;

import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.constant.SecurityConstants;
import com.winsalty.quickstart.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 令牌工具。
 * 负责签发和解析 access/refresh token。token 中只放身份、角色、sessionId 和类型，
 * refresh token 是否仍有效由 Redis 会话服务做二次校验。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Component
public class JwtTokenProvider {

    @Value("${app.security.jwt-secret}")
    private String secret;

    @Value("${app.security.jwt-expire-seconds}")
    private long accessExpireSeconds;

    @Value("${app.security.jwt-refresh-expire-seconds:604800}")
    private long refreshExpireSeconds;

    private Key key;

    /**
     * 启动时校验 HMAC 密钥长度，避免运行中签发弱密钥 token。
     */
    @PostConstruct
    public void init() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.security.jwt-secret 长度不足 32 字节，请检查配置");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成无横线 sessionId，用于关联一组 access/refresh token。
     */
    public String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public String createAccessToken(Long userId, String username, String roleCode, String sessionId) {
        return buildToken(userId, username, roleCode, sessionId, SecurityConstants.TOKEN_TYPE_ACCESS, accessExpireSeconds);
    }

    public String createRefreshToken(Long userId, String username, String roleCode, String sessionId) {
        return buildToken(userId, username, roleCode, sessionId, SecurityConstants.TOKEN_TYPE_REFRESH, refreshExpireSeconds);
    }

    /**
     * 解析 access token 并转换为过滤器可放入上下文的认证用户。
     */
    public AuthUser parseAccessToken(String token) {
        TokenPayload payload = parseToken(token);
        if (!SecurityConstants.TOKEN_TYPE_ACCESS.equals(payload.getTokenType())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        return new AuthUser(payload.getUserId(), payload.getUsername(), payload.getRoleCode(), payload.getSessionId());
    }

    /**
     * 解析 token 的通用 payload，签名、过期时间、格式错误都会统一转为业务异常。
     */
    public TokenPayload parseToken(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            Claims claims = claimsJws.getBody();
            return new TokenPayload(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("roleCode", String.class),
                    claims.get("sessionId", String.class),
                    claims.get("tokenType", String.class)
            );
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    public long getAccessExpireSeconds() {
        return accessExpireSeconds;
    }

    public long getRefreshExpireSeconds() {
        return refreshExpireSeconds;
    }

    private String buildToken(Long userId,
                              String username,
                              String roleCode,
                              String sessionId,
                              String tokenType,
                              long expireSeconds) {
        Date now = new Date();
        Date expiredAt = new Date(now.getTime() + expireSeconds * 1000);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("roleCode", roleCode)
                .claim("sessionId", sessionId)
                .claim("tokenType", tokenType)
                .setIssuedAt(now)
                .setExpiration(expiredAt)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
