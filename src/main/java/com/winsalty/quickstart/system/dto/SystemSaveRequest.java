package com.winsalty.quickstart.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 系统管理保存请求对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class SystemSaveRequest {

    private String id;

    @NotBlank(message = "moduleKey 不能为空")
    @Pattern(regexp = "users|roles|dicts|logs", message = "moduleKey 不合法")
    private String moduleKey;

    @NotBlank(message = "名称不能为空")
    @Size(max = 40, message = "名称长度不能超过 40")
    private String name;

    @NotBlank(message = "编码不能为空")
    @Size(max = 60, message = "编码长度不能超过 60")
    private String code;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;

    @NotBlank(message = "负责人不能为空")
    @Size(max = 30, message = "负责人长度不能超过 30")
    private String owner;

    @NotBlank(message = "描述不能为空")
    @Size(max = 160, message = "描述长度不能超过 160")
    private String description;

    @Size(max = 500, message = "头像地址长度不能超过 500")
    private String avatarUrl;

    @Size(max = 60, message = "扩展值长度不能超过 60")
    private String extraValue;

    private String departmentId;

    private java.util.List<String> roleCodes;
}
