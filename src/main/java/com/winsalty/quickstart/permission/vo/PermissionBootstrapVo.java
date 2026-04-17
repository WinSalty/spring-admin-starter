package com.winsalty.quickstart.permission.vo;

import java.util.List;

/**
 * 权限引导响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class PermissionBootstrapVo {

    private List<PermissionMenuVo> menus;
    private List<String> routes;
    private List<PermissionActionVo> actions;

    public List<PermissionMenuVo> getMenus() {
        return menus;
    }

    public void setMenus(List<PermissionMenuVo> menus) {
        this.menus = menus;
    }

    public List<String> getRoutes() {
        return routes;
    }

    public void setRoutes(List<String> routes) {
        this.routes = routes;
    }

    public List<PermissionActionVo> getActions() {
        return actions;
    }

    public void setActions(List<PermissionActionVo> actions) {
        this.actions = actions;
    }
}
