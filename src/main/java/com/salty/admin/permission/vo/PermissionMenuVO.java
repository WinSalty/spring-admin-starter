package com.salty.admin.permission.vo;

import java.util.ArrayList;
import java.util.List;

public class PermissionMenuVO {

    private String id;

    private String parentId;

    private String title;

    private String path;

    private String icon;

    private Integer orderNo;

    private String type;

    private String permissionCode;

    private Boolean hiddenInMenu;

    private String redirect;

    private Boolean keepAlive;

    private String externalLink;

    private String badge;

    private Boolean disabled;

    private List<PermissionMenuVO> children = new ArrayList<PermissionMenuVO>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public List<PermissionMenuVO> getChildren() {
        return children;
    }

    public void setChildren(List<PermissionMenuVO> children) {
        this.children = children;
    }
}
