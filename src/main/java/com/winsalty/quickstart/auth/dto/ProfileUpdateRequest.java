package com.winsalty.quickstart.auth.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 当前用户基本资料更新请求。
 * 创建日期：2026-04-19
 * author：sunshengxian
 */
@Data
public class ProfileUpdateRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128 个字符")
    private String email;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过 64 个字符")
    private String nickname;

    @Size(max = 255, message = "个人简介长度不能超过 255 个字符")
    private String description;

    @Size(max = 255, message = "头像地址长度不能超过 255 个字符")
    private String avatarUrl;

    @Size(max = 64, message = "国家/地区长度不能超过 64 个字符")
    private String country;

    @Size(max = 64, message = "省份长度不能超过 64 个字符")
    private String province;

    @Size(max = 64, message = "城市长度不能超过 64 个字符")
    private String city;

    @Size(max = 255, message = "街道地址长度不能超过 255 个字符")
    private String streetAddress;

    @Size(max = 12, message = "电话区号长度不能超过 12 个字符")
    private String phonePrefix;

    @Size(max = 32, message = "联系电话长度不能超过 32 个字符")
    private String phoneNumber;
}
