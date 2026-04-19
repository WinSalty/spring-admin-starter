package com.winsalty.quickstart.auth.vo;

import lombok.Data;

/**
 * 刷新令牌响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class RefreshTokenResponse {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private Long refreshExpiresIn;
    private String tokenType;
}
