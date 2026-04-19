package com.winsalty.quickstart.auth.vo;

import lombok.Data;

/**
 * 当前用户信息响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class ProfileResponse {

    private Long userId;
    private String username;
    private String email;
    private String nickname;
    private String description;
    private String avatarUrl;
    private String country;
    private String province;
    private String city;
    private String streetAddress;
    private String phonePrefix;
    private String phoneNumber;
    private Boolean notifyAccount;
    private Boolean notifySystem;
    private Boolean notifyTodo;
    private String roleCode;
    private String roleName;
}
