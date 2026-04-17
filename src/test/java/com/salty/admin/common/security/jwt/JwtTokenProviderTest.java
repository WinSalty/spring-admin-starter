package com.salty.admin.common.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderTest {

    @Test
    void createTokenAndParseReturnsExpectedClaims() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "configuredSecret", "test-secret-for-jwt-token-provider");
        provider.init();

        String token = provider.createToken(10L, "admin", JwtTokenProvider.TYPE_ACCESS, "web", "token-id-1", 60);
        JwtClaims claims = provider.parse(token);

        assertEquals(10L, claims.getUserId());
        assertEquals("admin", claims.getUsername());
        assertEquals(JwtTokenProvider.TYPE_ACCESS, claims.getType());
        assertEquals("web", claims.getDeviceId());
        assertEquals("token-id-1", claims.getTokenId());
        assertNotNull(claims.getExpiresAt());
    }

    @Test
    void initFailsWhenSecretIsMissing() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "configuredSecret", "");

        assertThrows(IllegalStateException.class, provider::init);
    }
}
