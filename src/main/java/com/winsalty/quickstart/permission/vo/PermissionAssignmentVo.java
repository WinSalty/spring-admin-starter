package com.winsalty.quickstart.permission.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色权限分配响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class PermissionAssignmentVo {

    private String roleCode;
    private List<String> menuIds = new ArrayList<String>();
    private List<String> routeCodes = new ArrayList<String>();
    private List<String> actionCodes = new ArrayList<String>();
}
