package com.salty.admin.permission.vo;

import java.util.ArrayList;
import java.util.List;

public class PermissionBootstrapVO {

    private List<PermissionMenuVO> menus = new ArrayList<PermissionMenuVO>();

    private List<String> routes = new ArrayList<String>();

    private List<PermissionActionVO> actions = new ArrayList<PermissionActionVO>();

    public List<PermissionMenuVO> getMenus() {
        return menus;
    }

    public void setMenus(List<PermissionMenuVO> menus) {
        this.menus = menus;
    }

    public List<String> getRoutes() {
        return routes;
    }

    public void setRoutes(List<String> routes) {
        this.routes = routes;
    }

    public List<PermissionActionVO> getActions() {
        return actions;
    }

    public void setActions(List<PermissionActionVO> actions) {
        this.actions = actions;
    }
}
