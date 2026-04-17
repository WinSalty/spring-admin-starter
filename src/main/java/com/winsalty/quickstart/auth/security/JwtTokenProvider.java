package com.winsalty.quickstart.auth.security;

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

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public String createAccessToken(Long userId, String username, String roleCode, String sessionId) {
        return buildToken(userId, username, roleCode, sessionId, "access", accessExpireSeconds);
    }

    public String createRefreshToken(Long userId, String username, String roleCode, String sessionId) {
        return buildToken(userId, username, roleCode, sessionId, "refresh", refreshExpireSeconds);
    }

    public AuthUser parseAccessToken(String token) {
        TokenPayload payload = parseToken(token);
        if (!"access".equals(payload.getTokenType())) {
            throw new BusinessException(4011, "登录令牌无效或已过期");
        }
        return new AuthUser(payload.getUserId(), payload.getUsername(), payload.getRoleCode(), payload.getSessionId());
    }

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
            throw new BusinessException(4011, "登录令牌无效或已过期");
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
