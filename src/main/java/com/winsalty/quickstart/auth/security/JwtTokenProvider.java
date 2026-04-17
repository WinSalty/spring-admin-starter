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
    private long expireSeconds;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(Long userId, String username, String roleCode) {
        Date now = new Date();
        Date expiredAt = new Date(now.getTime() + expireSeconds * 1000);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("roleCode", roleCode)
                .setIssuedAt(now)
                .setExpiration(expiredAt)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public AuthUser parseToken(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            Claims claims = claimsJws.getBody();
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);
            String roleCode = claims.get("roleCode", String.class);
            return new AuthUser(userId, username, roleCode);
        } catch (Exception exception) {
            throw new BusinessException(4011, "登录令牌无效或已过期");
        }
    }
}
