package com.winsalty.quickstart.system.entity;

/**
 * 菜单管理实体对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecordCode() {
        return recordCode;
    }

    public void setRecordCode(String recordCode) {
        this.recordCode = recordCode;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public String getRouteCode() {
        return routeCode;
    }

    public void setRouteCode(String routeCode) {
        this.routeCode = routeCode;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getExternalLink() {
        return externalLink;
    }

    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
