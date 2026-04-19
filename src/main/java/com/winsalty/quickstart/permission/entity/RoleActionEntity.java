package com.winsalty.quickstart.permission.entity;

import lombok.Data;

/**
 * 权限动作实体。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class RoleActionEntity {

    /** 角色ID。 */
    private Long roleId;
    /** 动作权限编码。 */
    private String actionCode;
    /** 动作权限名称。 */
    private String actionName;
}
