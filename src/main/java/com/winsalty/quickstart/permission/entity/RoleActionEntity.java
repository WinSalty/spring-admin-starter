package com.winsalty.quickstart.permission.entity;

/**
 * 权限动作实体。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class RoleActionEntity {

    /** 角色ID。 */
    private Long roleId;
    /** 动作权限编码。 */
    private String actionCode;
    /** 动作权限名称。 */
    private String actionName;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }
}
