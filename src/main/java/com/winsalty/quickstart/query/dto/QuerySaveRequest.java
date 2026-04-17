package com.winsalty.quickstart.query.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 查询配置保存请求对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class QuerySaveRequest {

    private String id;

    @NotBlank(message = "名称不能为空")
    @Size(max = 40, message = "名称长度不能超过 40")
    private String name;

    @NotBlank(message = "编码不能为空")
    @Size(max = 60, message = "编码长度不能超过 60")
    private String code;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;

    @NotBlank(message = "负责人不能为空")
    @Size(max = 30, message = "负责人长度不能超过 30")
    private String owner;

    @NotBlank(message = "描述不能为空")
    @Size(max = 160, message = "描述长度不能超过 160")
    private String description;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
