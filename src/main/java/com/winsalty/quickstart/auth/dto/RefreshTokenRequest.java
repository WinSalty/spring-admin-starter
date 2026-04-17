package com.winsalty.quickstart.auth.dto;

import javax.validation.constraints.NotBlank;

/**
 * 刷新令牌请求对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
