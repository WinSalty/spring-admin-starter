package com.salty.admin.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.salty.admin.auth.entity.SysRefreshToken;
import com.salty.admin.auth.entity.SysUser;
import com.salty.admin.auth.mapper.SysRefreshTokenMapper;
import com.salty.admin.auth.mapper.SysUserMapper;
import com.salty.admin.auth.vo.LoginResponseVO;
import com.salty.admin.auth.vo.UserInfoVO;
import com.salty.admin.common.enums.ErrorCode;
import com.salty.admin.common.exception.BusinessException;
import com.salty.admin.common.security.LoginUser;
import com.salty.admin.common.security.jwt.JwtClaims;
import com.salty.admin.common.security.jwt.JwtTokenProvider;
import com.salty.admin.permission.service.PermissionService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    public static final long ACCESS_TTL_SECONDS = 2 * 60 * 60L;

    public static final long REFRESH_TTL_SECONDS = 7 * 24 * 60 * 60L;

    private final JwtTokenProvider jwtTokenProvider;

    private final SysRefreshTokenMapper refreshTokenMapper;

    private final SysUserMapper userMapper;

    private final PermissionService permissionService;

    private final StringRedisTemplate redisTemplate;

    private final HashService hashService;

    private static final int REFRESH_IP_LIMIT_PER_MINUTE = 10;

    public TokenService(JwtTokenProvider jwtTokenProvider,
                        SysRefreshTokenMapper refreshTokenMapper,
                        SysUserMapper userMapper,
                        PermissionService permissionService,
                        StringRedisTemplate redisTemplate,
                        HashService hashService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenMapper = refreshTokenMapper;
        this.userMapper = userMapper;
        this.permissionService = permissionService;
        this.redisTemplate = redisTemplate;
        this.hashService = hashService;
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginResponseVO issue(SysUser user, String deviceId, String deviceName) {
        String normalizedDeviceId = StringUtils.hasText(deviceId) ? deviceId : "web";
        String accessTokenId = jwtTokenProvider.newTokenId();
        String refreshTokenId = jwtTokenProvider.newTokenId();
        String accessToken = jwtTokenProvider.createToken(user.getId(), user.getUsername(), JwtTokenProvider.TYPE_ACCESS,
                normalizedDeviceId, accessTokenId, ACCESS_TTL_SECONDS);
        String refreshToken = jwtTokenProvider.createToken(user.getId(), user.getUsername(), JwtTokenProvider.TYPE_REFRESH,
                normalizedDeviceId, refreshTokenId, REFRESH_TTL_SECONDS);

        redisTemplate.opsForValue().set(accessKey(accessTokenId), String.valueOf(user.getId()), ACCESS_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(refreshKey(refreshTokenId), String.valueOf(user.getId()), REFRESH_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(accessRefreshKey(accessTokenId), refreshTokenId, ACCESS_TTL_SECONDS, TimeUnit.SECONDS);

        SysRefreshToken record = new SysRefreshToken();
        record.setUserId(user.getId());
        record.setTokenId(refreshTokenId);
        record.setTokenHash(hashService.sha256(refreshToken));
        record.setDeviceId(normalizedDeviceId);
        record.setDeviceName(deviceName);
        record.setExpireTime(LocalDateTime.now().plusSeconds(REFRESH_TTL_SECONDS));
        record.setRevoked(0);
        refreshTokenMapper.insert(record);

        LoginResponseVO response = new LoginResponseVO();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(ACCESS_TTL_SECONDS);
        response.setUser(toUserInfo(user));
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginResponseVO refresh(String refreshToken) {
        return refresh(refreshToken, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginResponseVO refresh(String refreshToken, String ip) {
        checkRefreshRateLimit(ip);
        JwtClaims claims = parseAndEnsureType(refreshToken, JwtTokenProvider.TYPE_REFRESH);
        if (!Boolean.TRUE.equals(redisTemplate.delete(refreshKey(claims.getTokenId())))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        SysRefreshToken record = refreshTokenMapper.selectOne(new LambdaQueryWrapper<SysRefreshToken>()
                .eq(SysRefreshToken::getTokenId, claims.getTokenId())
                .last("LIMIT 1"));
        if (record == null || Integer.valueOf(1).equals(record.getRevoked())
                || record.getExpireTime().isBefore(LocalDateTime.now())
                || !hashService.sha256(refreshToken).equals(record.getTokenHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        record.setRevoked(1);
        record.setRevokedTime(LocalDateTime.now());
        refreshTokenMapper.updateById(record);

        SysUser user = userMapper.selectById(record.getUserId());
        if (user == null || Integer.valueOf(1).equals(user.getDeleted()) || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return issue(user, record.getDeviceId(), record.getDeviceName());
    }

    @Transactional(rollbackFor = Exception.class)
    public void logout(String accessToken) {
        JwtClaims claims = parseAndEnsureType(accessToken, JwtTokenProvider.TYPE_ACCESS);
        redisTemplate.delete(accessKey(claims.getTokenId()));
        String refreshTokenId = redisTemplate.opsForValue().get(accessRefreshKey(claims.getTokenId()));
        redisTemplate.delete(accessRefreshKey(claims.getTokenId()));
        if (StringUtils.hasText(refreshTokenId)) {
            redisTemplate.delete(refreshKey(refreshTokenId));
            SysRefreshToken record = refreshTokenMapper.selectOne(new LambdaQueryWrapper<SysRefreshToken>()
                    .eq(SysRefreshToken::getTokenId, refreshTokenId)
                    .last("LIMIT 1"));
            if (record != null && !Integer.valueOf(1).equals(record.getRevoked())) {
                record.setRevoked(1);
                record.setRevokedTime(LocalDateTime.now());
                refreshTokenMapper.updateById(record);
            }
        }
    }

    public LoginUser authenticateAccessToken(String accessToken) {
        JwtClaims claims = parseAndEnsureType(accessToken, JwtTokenProvider.TYPE_ACCESS);
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(accessKey(claims.getTokenId())))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        SysUser user = userMapper.selectById(claims.getUserId());
        if (user == null || Integer.valueOf(1).equals(user.getDeleted()) || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setEmail(user.getEmail());
        loginUser.setNickName(user.getNickName());
        loginUser.setStatus(user.getStatus());
        loginUser.setDeviceId(claims.getDeviceId());
        loginUser.setTokenId(claims.getTokenId());
        loginUser.setRoles(permissionService.listRoleCodes(user.getId()));
        loginUser.setPermissions(permissionService.listPermissionCodes(user.getId()));
        return loginUser;
    }

    public UserInfoVO toUserInfo(SysUser user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setNickName(user.getNickName());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRoles(new ArrayList<String>(permissionService.listRoleCodes(user.getId())));
        vo.setActions(new ArrayList<String>(permissionService.listPermissionCodes(user.getId())));
        Collections.sort(vo.getRoles());
        Collections.sort(vo.getActions());
        return vo;
    }

    public String resolveBearer(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }

    private JwtClaims parseAndEnsureType(String token, String type) {
        try {
            JwtClaims claims = jwtTokenProvider.parse(token);
            if (!type.equals(claims.getType())) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            return claims;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private String accessKey(String tokenId) {
        return "auth:token:access:" + tokenId;
    }

    private String refreshKey(String tokenId) {
        return "auth:token:refresh:" + tokenId;
    }

    private String accessRefreshKey(String tokenId) {
        return "auth:token:access-refresh:" + tokenId;
    }

    private void checkRefreshRateLimit(String ip) {
        String key = "auth:refresh:limit:ip:" + (StringUtils.hasText(ip) ? ip : "unknown");
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }
        if (count != null && count > REFRESH_IP_LIMIT_PER_MINUTE) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "refresh token 请求过于频繁");
        }
    }
}
