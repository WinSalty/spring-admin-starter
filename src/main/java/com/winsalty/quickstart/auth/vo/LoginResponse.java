package com.winsalty.quickstart.auth.vo;

import lombok.Data;

/**
 * 登录响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class LoginResponse {

    private String token;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private Long refreshExpiresIn;
    private String tokenType;
    private String roleCode;
    private String roleName;

    public LoginResponse() {
    }

    public LoginResponse(String token, String accessToken, String refreshToken, Long expiresIn, Long refreshExpiresIn, String tokenType) {
        this.token = token;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
        this.tokenType = tokenType;
    }
}
