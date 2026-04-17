package com.salty.admin.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    public static final String TYPE_ACCESS = "access";

    public static final String TYPE_REFRESH = "refresh";

    @Value("${admin.security.jwt-secret:}")
    private String configuredSecret;

    private byte[] secretKey;

    @PostConstruct
    public void init() {
        String envSecret = System.getenv("ADMIN_JWT_SECRET");
        String secret = StringUtils.hasText(envSecret) ? envSecret : configuredSecret;
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT secret is not configured. Set ADMIN_JWT_SECRET or admin.security.jwt-secret before starting the application.");
        }
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String createToken(Long userId, String username, String type, String deviceId, String tokenId, long ttlSeconds) {
        Date now = new Date();
        Date expires = new Date(now.getTime() + ttlSeconds * 1000L);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setId(tokenId)
                .claim("typ", type)
                .claim("username", username)
                .claim("deviceId", deviceId)
                .claim("permVersion", 1)
                .setIssuedAt(now)
                .setExpiration(expires)
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    public String newTokenId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public JwtClaims parse(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
        JwtClaims result = new JwtClaims();
        result.setUserId(Long.valueOf(claims.getSubject()));
        result.setUsername(claims.get("username", String.class));
        result.setType(claims.get("typ", String.class));
        result.setDeviceId(claims.get("deviceId", String.class));
        result.setTokenId(claims.getId());
        result.setExpiresAt(claims.getExpiration().getTime());
        return result;
    }
}
