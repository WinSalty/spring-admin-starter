package com.winsalty.quickstart.auth.service;

import com.winsalty.quickstart.auth.dto.LoginRequest;
import com.winsalty.quickstart.auth.dto.RefreshTokenRequest;
import com.winsalty.quickstart.auth.dto.RegisterRequest;
import com.winsalty.quickstart.auth.vo.LoginResponse;
import com.winsalty.quickstart.auth.vo.ProfileResponse;
import com.winsalty.quickstart.auth.vo.RefreshTokenResponse;

/**
 * 认证服务接口。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public interface AuthService {

    LoginResponse login(LoginRequest request);

    RefreshTokenResponse refreshToken(RefreshTokenRequest request);

    void logout(Long userId, String sessionId);

    void register(RegisterRequest request);

    ProfileResponse getProfile(Long userId);
}
