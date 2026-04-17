package com.winsalty.quickstart.permission.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色权限分配响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class PermissionAssignmentVo {

    private String roleCode;
    private List<String> menuIds = new ArrayList<String>();
    private List<String> routeCodes = new ArrayList<String>();
    private List<String> actionCodes = new ArrayList<String>();

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public List<String> getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(List<String> menuIds) {
        this.menuIds = menuIds;
    }

    public List<String> getRouteCodes() {
        return routeCodes;
    }

    public void setRouteCodes(List<String> routeCodes) {
        this.routeCodes = routeCodes;
    }

    public List<String> getActionCodes() {
        return actionCodes;
    }

    public void setActionCodes(List<String> actionCodes) {
        this.actionCodes = actionCodes;
    }
}
