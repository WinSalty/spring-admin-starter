package com.winsalty.quickstart.auth.controller;

import com.winsalty.quickstart.auth.dto.LoginRequest;
import com.winsalty.quickstart.auth.dto.RegisterRequest;
import com.winsalty.quickstart.auth.security.AuthContext;
import com.winsalty.quickstart.auth.security.AuthUser;
import com.winsalty.quickstart.auth.service.AuthService;
import com.winsalty.quickstart.auth.vo.LoginResponse;
import com.winsalty.quickstart.auth.vo.ProfileResponse;
import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${app.security.register-enabled:false}")
    private boolean registerEnabled;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<Object> register(@Validated @RequestBody RegisterRequest request) {
        if (!registerEnabled) {
            throw new BusinessException(4031, "当前环境不开放注册");
        }
        authService.register(request);
        return ApiResponse.success("注册成功", null);
    }

    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> profile() {
        AuthUser authUser = AuthContext.get();
        if (authUser == null) {
            throw new BusinessException(4010, "未登录或登录已失效");
        }
        return ApiResponse.success(authService.getProfile(authUser.getUserId()));
    }
}
