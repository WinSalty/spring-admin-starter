package com.winsalty.quickstart.department.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门响应对象。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<DepartmentVo> getChildren() {
        return children;
    }

    public void setChildren(List<DepartmentVo> children) {
        this.children = children;
    }
}
