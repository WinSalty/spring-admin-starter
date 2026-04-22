package com.winsalty.quickstart.auth.controller;

import com.winsalty.quickstart.auth.annotation.AuditLog;
import com.winsalty.quickstart.auth.dto.LoginRequest;
import com.winsalty.quickstart.auth.dto.NotificationSettingsRequest;
import com.winsalty.quickstart.auth.dto.PasswordUpdateRequest;
import com.winsalty.quickstart.auth.dto.ProfileUpdateRequest;
import com.winsalty.quickstart.auth.dto.RefreshTokenRequest;
import com.winsalty.quickstart.auth.dto.RegisterRequest;
import com.winsalty.quickstart.auth.security.AuthUser;
import com.winsalty.quickstart.auth.service.AuthService;
import com.winsalty.quickstart.auth.service.support.AuthRateLimitService;
import com.winsalty.quickstart.auth.vo.LoginResponse;
import com.winsalty.quickstart.auth.vo.ProfileResponse;
import com.winsalty.quickstart.auth.vo.RefreshTokenResponse;
import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.base.BaseController;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.common.util.IpUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * 认证控制器。
 * 暴露登录、刷新令牌、登出、注册和个人资料接口。
 * 除登录/注册/刷新外，其余接口均依赖 JwtAuthenticationFilter 注入当前用户。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController extends BaseController {

    private final AuthService authService;
    private final AuthRateLimitService authRateLimitService;

    @Value("${app.security.register-enabled:false}")
    private boolean registerEnabled;

    public AuthController(AuthService authService, AuthRateLimitService authRateLimitService) {
        this.authService = authService;
        this.authRateLimitService = authRateLimitService;
    }

    /**
     * 登录成功后返回 access token、refresh token 和前端兼容字段 token。
     */
    @AuditLog(logType = "login", code = "auth_login", name = "用户登录")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Validated @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        authRateLimitService.checkLogin(request.getUsername(), IpUtils.getClientIp(servletRequest));
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
     * 登出失效当前 sessionId；过滤器会拒绝该 session 后续携带的 access token。
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
     * 注册验证码发送入口。未注册用户必须能匿名访问，验证码只通过邮件送达。
     */
    @GetMapping("/register/verify-code")
    public ApiResponse<Object> registerVerifyCode(@NotBlank(message = "邮箱不能为空")
                                                  @Email(message = "邮箱格式不正确")
                                                  @RequestParam("email") String email,
                                                  HttpServletRequest servletRequest) {
        authRateLimitService.checkRegisterVerifyCode(email, IpUtils.getClientIp(servletRequest));
        authService.sendRegisterVerifyCode(email);
        return ApiResponse.success("发送成功", null);
    }

    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> profile() {
        return ApiResponse.success(authService.getProfile(currentUserId()));
    }

    @AuditLog(logType = "operation", code = "auth_profile_update", name = "更新个人资料")
    @PutMapping("/profile")
    public ApiResponse<ProfileResponse> updateProfile(@Validated @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.success("保存成功", authService.updateProfile(currentUserId(), request));
    }

    @AuditLog(logType = "operation", code = "auth_password_update", name = "修改登录密码")
    @PostMapping("/password")
    public ApiResponse<Object> updatePassword(@Validated @RequestBody PasswordUpdateRequest request) {
        authService.updatePassword(currentUserId(), request);
        return ApiResponse.success("密码已更新", null);
    }

    @AuditLog(logType = "operation", code = "auth_notification_update", name = "更新通知设置")
    @PutMapping("/profile/notifications")
    public ApiResponse<ProfileResponse> updateNotificationSettings(@Validated @RequestBody NotificationSettingsRequest request) {
        return ApiResponse.success("通知设置已保存", authService.updateNotificationSettings(currentUserId(), request));
    }
}
