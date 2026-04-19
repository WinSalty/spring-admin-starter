package com.winsalty.quickstart.system.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 菜单保存请求对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class SystemMenuSaveRequest {

    private String id;

    @Size(max = 32, message = "parentId 长度不能超过 32")
    private String parentId;

    @NotBlank(message = "名称不能为空")
    @Size(max = 40, message = "名称长度不能超过 40")
    private String name;

    @NotBlank(message = "编码不能为空")
    @Size(max = 60, message = "编码长度不能超过 60")
    private String code;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;

    @NotBlank(message = "描述不能为空")
    @Size(max = 160, message = "描述长度不能超过 160")
    private String description;

    @NotBlank(message = "菜单类型不能为空")
    @Pattern(regexp = "catalog|menu|hidden|external", message = "菜单类型不合法")
    private String menuType;

    @Size(max = 64, message = "图标长度不能超过 64")
    private String icon;

    @Size(max = 128, message = "路由路径长度不能超过 128")
    private String routePath;

    @Size(max = 128, message = "权限编码长度不能超过 128")
    private String permissionCode;

    @Size(max = 255, message = "外链地址长度不能超过 255")
    private String externalLink;

    @NotNull(message = "排序不能为空")
    @Min(value = 1, message = "排序不能小于 1")
    @Max(value = 9999, message = "排序不能大于 9999")
    private Integer orderNo;
}
