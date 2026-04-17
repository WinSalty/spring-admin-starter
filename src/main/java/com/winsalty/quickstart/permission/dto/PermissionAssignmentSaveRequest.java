package com.winsalty.quickstart.permission.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限分配保存请求对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class PermissionAssignmentSaveRequest {

    @NotBlank(message = "roleCode 不能为空")
    @Size(max = 64, message = "roleCode 长度不能超过 64")
    private String roleCode;

    @NotNull(message = "menuIds 不能为空")
    private List<String> menuIds = new ArrayList<String>();

    @NotNull(message = "routeCodes 不能为空")
    private List<String> routeCodes = new ArrayList<String>();

    @NotNull(message = "actionCodes 不能为空")
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
