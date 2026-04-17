package com.winsalty.quickstart.auth.vo;

/**
 * 登录响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class LoginResponse {

    private String token;

    public LoginResponse() {
    }

    public LoginResponse(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
