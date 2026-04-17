package com.salty.admin.auth.controller;

import com.salty.admin.auth.dto.LoginRequest;
import com.salty.admin.auth.dto.RegisterRequest;
import com.salty.admin.auth.dto.SendEmailCodeRequest;
import com.salty.admin.auth.service.AuthService;
import com.salty.admin.auth.service.EmailCodeService;
import com.salty.admin.auth.service.TokenService;
import com.salty.admin.auth.vo.LoginResponseVO;
import com.salty.admin.auth.vo.UserInfoVO;
import com.salty.admin.common.api.ApiResponse;
import com.salty.admin.common.enums.ErrorCode;
import com.salty.admin.common.exception.BusinessException;
import com.salty.admin.common.security.SecurityUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    private final EmailCodeService emailCodeService;

    private final TokenService tokenService;

    public AuthController(AuthService authService, EmailCodeService emailCodeService, TokenService tokenService) {
        this.authService = authService;
        this.emailCodeService = emailCodeService;
        this.tokenService = tokenService;
    }

    @PostMapping("/email-code/send")
    public ApiResponse<Void> sendEmailCode(@Valid @RequestBody SendEmailCodeRequest request, HttpServletRequest servletRequest) {
        emailCodeService.sendRegisterCode(request.getEmail(), clientIp(servletRequest));
        return ApiResponse.success();
    }

    @PostMapping("/register")
    public ApiResponse<UserInfoVO> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.register(request, clientIp(servletRequest)));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponseVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.login(request, clientIp(servletRequest), servletRequest.getHeader("User-Agent")));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponseVO> refresh(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                HttpServletRequest servletRequest) {
        String refreshToken = tokenService.resolveBearer(authorization);
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.success(tokenService.refresh(refreshToken, clientIp(servletRequest)));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String accessToken = tokenService.resolveBearer(authorization);
        if (!StringUtils.hasText(accessToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        tokenService.logout(accessToken);
        return ApiResponse.success();
    }

    @GetMapping("/profile")
    public ApiResponse<UserInfoVO> profile() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.success(authService.profile(userId));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? realIp : request.getRemoteAddr();
    }
}
