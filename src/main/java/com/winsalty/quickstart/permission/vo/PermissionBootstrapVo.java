package com.winsalty.quickstart.permission.vo;

import lombok.Data;

import java.util.List;

/**
 * 权限引导响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class PermissionBootstrapVo {

    private List<PermissionMenuVo> menus;
    private List<String> routes;
    private List<PermissionActionVo> actions;
}
