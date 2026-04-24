package com.winsalty.quickstart.permission.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单权限响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class PermissionMenuVo {

    private String id;
    private String parentId;
    private String title;
    private String path;
    private String icon;
    private Integer orderNo;
    private String type;
    private String routeCode;
    private String permissionCode;
    private Boolean hiddenInMenu;
    private String redirect;
    private Boolean keepAlive;
    private String externalLink;
    private String badge;
    private Boolean disabled;
    private List<PermissionMenuVo> children = new ArrayList<PermissionMenuVo>();
}
