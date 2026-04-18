package com.winsalty.quickstart.permission.entity;

/**
 * 菜单实体。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
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

    public Boolean getHiddenInMenu() {
        return hiddenInMenu;
    }

    public void setHiddenInMenu(Boolean hiddenInMenu) {
        this.hiddenInMenu = hiddenInMenu;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }

    public Boolean getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(Boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public String getExternalLink() {
        return externalLink;
    }

    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
