package com.winsalty.quickstart.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 当前用户密码更新请求。
 * 创建日期：2026-04-19
 * author：sunshengxian
 */
@Data
public class PasswordUpdateRequest {

    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "新密码长度需为 6-32 个字符")
    private String newPassword;
}
