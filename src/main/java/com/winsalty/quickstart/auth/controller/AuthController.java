package com.winsalty.quickstart.auth.controller;

import com.winsalty.quickstart.auth.annotation.AuditLog;
import com.winsalty.quickstart.auth.dto.LoginRequest;
import com.winsalty.quickstart.auth.dto.RefreshTokenRequest;
import com.winsalty.quickstart.auth.dto.RegisterRequest;
import com.winsalty.quickstart.auth.security.AuthUser;
import com.winsalty.quickstart.auth.service.AuthService;
import com.winsalty.quickstart.auth.vo.LoginResponse;
import com.winsalty.quickstart.auth.vo.ProfileResponse;
import com.winsalty.quickstart.auth.vo.RefreshTokenResponse;
import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.base.BaseController;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器。
 * 暴露登录、刷新令牌、登出、注册和个人资料接口。
 * 除登录/注册/刷新外，其余接口均依赖 JwtAuthenticationFilter 注入当前用户。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController extends BaseController {

    private final AuthService authService;

    @Value("${app.security.register-enabled:false}")
    private boolean registerEnabled;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 登录成功后返回 access token、refresh token 和前端兼容字段 token。
     */
    @AuditLog(logType = "login", code = "auth_login", name = "用户登录")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    /**
     * 使用 refresh token 换取新的一组令牌，同时轮换 Redis 中保存的 refresh token。
     */
    @AuditLog(logType = "api", code = "auth_refresh_token", name = "刷新令牌")
    @PostMapping("/refresh-token")
    public ApiResponse<RefreshTokenResponse> refreshToken(@Validated @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success("刷新成功", authService.refreshToken(request));
    }

    /**
     * 登出只失效当前 sessionId，不影响同账号其他设备或浏览器会话。
     */
    @AuditLog(logType = "operation", code = "auth_logout", name = "用户退出")
    @PostMapping("/logout")
    public ApiResponse<Object> logout() {
        AuthUser authUser = requireCurrentUser();
        authService.logout(authUser.getUserId(), authUser.getSessionId());
        return ApiResponse.success("退出成功", null);
    }

    /**
     * 注册入口由 app.security.register-enabled 控制，生产环境默认关闭。
     */
    @AuditLog(logType = "operation", code = "auth_register", name = "用户注册")
    @PostMapping("/register")
    public ApiResponse<Object> register(@Validated @RequestBody RegisterRequest request) {
        if (!registerEnabled) {
            throw new BusinessException(ErrorCode.REGISTER_DISABLED);
        }
        authService.register(request);
        return ApiResponse.success("注册成功", null);
    }

    /**
     * 开发阶段直接返回验证码，后续接入邮件服务时可改为只返回发送结果。
     */
    @GetMapping("/register/verify-code")
    public ApiResponse<String> registerVerifyCode(@RequestParam("email") String email) {
        return ApiResponse.success("发送成功", authService.generateRegisterVerifyCode(email));
    }

    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> profile() {
        return ApiResponse.success(authService.getProfile(currentUserId()));
    }
}
