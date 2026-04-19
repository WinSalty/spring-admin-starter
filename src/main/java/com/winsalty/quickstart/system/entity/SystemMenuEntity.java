package com.winsalty.quickstart.system.entity;

import lombok.Data;

/**
 * 菜单管理实体对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class SystemMenuEntity {

    /** 菜单主键ID。 */
    private Long id;
    /** 菜单记录编码。 */
    private String recordCode;
    /** 父级菜单ID。 */
    private Long parentId;
    /** 菜单名称。 */
    private String name;
    /** 菜单编码。 */
    private String code;
    /** 菜单状态。 */
    private String status;
    /** 负责人。 */
    private String owner;
    /** 描述信息。 */
    private String description;
    /** 菜单类型。 */
    private String menuType;
    /** 图标名称。 */
    private String icon;
    /** 路由路径。 */
    private String routePath;
    /** 路由权限码。 */
    private String routeCode;
    /** 权限编码。 */
    private String permissionCode;
    /** 外链地址。 */
    private String externalLink;
    /** 排序号。 */
    private Integer orderNo;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
