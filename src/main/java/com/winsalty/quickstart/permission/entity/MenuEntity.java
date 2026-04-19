package com.winsalty.quickstart.permission.entity;

import lombok.Data;

/**
 * 菜单实体。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class MenuEntity {

    /** 菜单主键ID。 */
    private Long id;
    /** 父级菜单ID。 */
    private Long parentId;
    /** 菜单标题。 */
    private String title;
    /** 菜单路径。 */
    private String path;
    /** 图标名称。 */
    private String icon;
    /** 排序号。 */
    private Integer orderNo;
    /** 菜单类型。 */
    private String menuType;
    /** 路由权限码。 */
    private String routeCode;
    /** 按钮或菜单权限码。 */
    private String permissionCode;
    /** 是否在菜单中隐藏。 */
    private Boolean hiddenInMenu;
    /** 默认跳转地址。 */
    private String redirect;
    /** 是否启用缓存。 */
    private Boolean keepAlive;
    /** 外链地址。 */
    private String externalLink;
    /** 菜单徽标。 */
    private String badge;
    /** 是否禁用。 */
    private Boolean disabled;
    /** 菜单状态。 */
    private String status;
}
