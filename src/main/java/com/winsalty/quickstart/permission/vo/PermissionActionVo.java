package com.winsalty.quickstart.permission.vo;

/**
 * 按钮权限响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class PermissionActionVo {

    private String code;
    private String name;

    public PermissionActionVo() {
    }

    public PermissionActionVo(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
