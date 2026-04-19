package com.winsalty.quickstart.permission.vo;

import lombok.Data;

/**
 * 按钮权限响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class PermissionActionVo {

    private String code;
    private String name;

    public PermissionActionVo() {
    }

    public PermissionActionVo(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
