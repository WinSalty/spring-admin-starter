package com.salty.admin.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.salty.admin.auth.entity.SysRefreshToken;
import com.salty.admin.auth.entity.SysUser;
import com.salty.admin.auth.mapper.SysRefreshTokenMapper;
import com.salty.admin.auth.mapper.SysUserMapper;
import com.salty.admin.auth.vo.LoginResponseVO;
import com.salty.admin.common.exception.BusinessException;
import com.salty.admin.common.security.jwt.JwtClaims;
import com.salty.admin.common.security.jwt.JwtTokenProvider;
import com.salty.admin.permission.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    private JwtTokenProvider jwtTokenProvider;

    private SysRefreshTokenMapper refreshTokenMapper;

    private SysUserMapper userMapper;

    private PermissionService permissionService;

    private StringRedisTemplate redisTemplate;

    private ValueOperations<String, String> valueOperations;

    private TokenService tokenService;

    private final HashService hashService = new HashService();

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "configuredSecret", "test-secret-for-token-service");
        jwtTokenProvider.init();
        refreshTokenMapper = mock(SysRefreshTokenMapper.class);
        userMapper = mock(SysUserMapper.class);
        permissionService = mock(PermissionService.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(permissionService.listRoleCodes(any())).thenReturn(Collections.emptySet());
        when(permissionService.listPermissionCodes(any())).thenReturn(Collections.emptySet());
        tokenService = new TokenService(jwtTokenProvider, refreshTokenMapper, userMapper, permissionService, redisTemplate, hashService);
    }

    @Test
    void issueCreatesAccessAndRefreshTokensAndStoresRefreshHash() {
        SysUser user = activeUser();

        LoginResponseVO response = tokenService.issue(user, "browser-1", "Chrome");

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals(TokenService.ACCESS_TTL_SECONDS, response.getExpiresIn());
        JwtClaims accessClaims = jwtTokenProvider.parse(response.getAccessToken());
        JwtClaims refreshClaims = jwtTokenProvider.parse(response.getRefreshToken());
        assertEquals("access", accessClaims.getType());
        assertEquals("refresh", refreshClaims.getType());

        ArgumentCaptor<SysRefreshToken> captor = ArgumentCaptor.forClass(SysRefreshToken.class);
        verify(refreshTokenMapper).insert(captor.capture());
        SysRefreshToken record = captor.getValue();
        assertEquals(user.getId(), record.getUserId());
        assertEquals(refreshClaims.getTokenId(), record.getTokenId());
        assertEquals(hashService.sha256(response.getRefreshToken()), record.getTokenHash());
        assertNotEquals(response.getRefreshToken(), record.getTokenHash());
        assertEquals(0, record.getRevoked());
    }

    @Test
    void refreshConsumesOldRefreshTokenBeforeIssuingNewTokens() {
        SysUser user = activeUser();
        LoginResponseVO original = tokenService.issue(user, "browser-1", "Chrome");
        JwtClaims refreshClaims = jwtTokenProvider.parse(original.getRefreshToken());
        SysRefreshToken oldRecord = refreshRecord(user.getId(), refreshClaims.getTokenId(), original.getRefreshToken());
        clearInvocations(refreshTokenMapper);
        when(redisTemplate.delete("auth:token:refresh:" + refreshClaims.getTokenId())).thenReturn(true);
        when(refreshTokenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(oldRecord);
        when(userMapper.selectById(user.getId())).thenReturn(user);

        LoginResponseVO refreshed = tokenService.refresh(original.getRefreshToken(), "127.0.0.1");

        assertNotNull(refreshed.getAccessToken());
        assertNotNull(refreshed.getRefreshToken());
        assertNotEquals(original.getRefreshToken(), refreshed.getRefreshToken());
        assertEquals(1, oldRecord.getRevoked());
        assertNotNull(oldRecord.getRevokedTime());
        InOrder order = inOrder(refreshTokenMapper);
        order.verify(refreshTokenMapper).updateById(oldRecord);
        order.verify(refreshTokenMapper).insert(any(SysRefreshToken.class));
    }

    @Test
    void refreshRejectsAlreadyConsumedRefreshToken() {
        SysUser user = activeUser();
        LoginResponseVO original = tokenService.issue(user, "browser-1", "Chrome");
        JwtClaims refreshClaims = jwtTokenProvider.parse(original.getRefreshToken());
        when(redisTemplate.delete("auth:token:refresh:" + refreshClaims.getTokenId())).thenReturn(false);

        assertThrows(BusinessException.class, () -> tokenService.refresh(original.getRefreshToken(), "127.0.0.1"));
    }

    @Test
    void refreshRateLimitRejectsMoreThanTenRequestsPerMinuteForSameIp() {
        SysUser user = activeUser();
        LoginResponseVO original = tokenService.issue(user, "browser-1", "Chrome");
        when(valueOperations.increment("auth:refresh:limit:ip:127.0.0.1")).thenReturn(11L);

        assertThrows(BusinessException.class, () -> tokenService.refresh(original.getRefreshToken(), "127.0.0.1"));
    }

    @Test
    void logoutDeletesSessionKeysAndRevokesRefreshToken() {
        SysUser user = activeUser();
        LoginResponseVO response = tokenService.issue(user, "browser-1", "Chrome");
        JwtClaims accessClaims = jwtTokenProvider.parse(response.getAccessToken());
        JwtClaims refreshClaims = jwtTokenProvider.parse(response.getRefreshToken());
        SysRefreshToken record = refreshRecord(user.getId(), refreshClaims.getTokenId(), response.getRefreshToken());
        when(valueOperations.get("auth:token:access-refresh:" + accessClaims.getTokenId())).thenReturn(refreshClaims.getTokenId());
        when(refreshTokenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

        tokenService.logout(response.getAccessToken());

        verify(redisTemplate).delete("auth:token:access:" + accessClaims.getTokenId());
        verify(redisTemplate).delete("auth:token:refresh:" + refreshClaims.getTokenId());
        assertEquals(1, record.getRevoked());
        assertNotNull(record.getRevokedTime());
        verify(refreshTokenMapper).updateById(record);
    }

    private SysUser activeUser() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setEmail("admin@example.com");
        user.setNickName("Admin");
        user.setStatus(1);
        user.setDeleted(0);
        return user;
    }

    private SysRefreshToken refreshRecord(Long userId, String tokenId, String refreshToken) {
        SysRefreshToken record = new SysRefreshToken();
        record.setId(100L);
        record.setUserId(userId);
        record.setTokenId(tokenId);
        record.setTokenHash(hashService.sha256(refreshToken));
        record.setDeviceId("browser-1");
        record.setDeviceName("Chrome");
        record.setExpireTime(LocalDateTime.now().plusMinutes(30));
        record.setRevoked(0);
        return record;
    }
}
