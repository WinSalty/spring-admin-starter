package com.winsalty.quickstart.department.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门响应对象。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Data
public class DepartmentVo {

    private String id;
    private String name;
    private String code;
    private String parentId;
    private Integer sortOrder;
    private String leader;
    private String phone;
    private String email;
    private String status;
    private String createdAt;
    private String updatedAt;
    private List<DepartmentVo> children = new ArrayList<DepartmentVo>();
}
