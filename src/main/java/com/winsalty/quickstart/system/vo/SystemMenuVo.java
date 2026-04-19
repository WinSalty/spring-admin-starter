package com.winsalty.quickstart.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单管理响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class SystemMenuVo {

    private String id;
    private String moduleKey;
    private String name;
    private String code;
    private String status;
    private String owner;
    private String description;
    private String parentId;
    private String menuType;
    private String icon;
    private String routePath;
    private String permissionCode;
    private String externalLink;
    private Integer orderNo;
    private String createdAt;
    private String updatedAt;
    private List<SystemMenuVo> children = new ArrayList<SystemMenuVo>();
}
