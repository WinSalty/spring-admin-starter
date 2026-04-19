package com.winsalty.quickstart.department.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 部门保存请求。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Data
public class DepartmentSaveRequest {

    private String id;
    private String parentId;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 64, message = "部门名称长度不能超过 64")
    private String name;

    @NotBlank(message = "部门编码不能为空")
    @Size(max = 64, message = "部门编码长度不能超过 64")
    private String code;

    @Min(value = 0, message = "排序不能小于 0")
    private Integer sortOrder = 0;

    @Size(max = 64, message = "负责人长度不能超过 64")
    private String leader;

    @Size(max = 32, message = "电话长度不能超过 32")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128")
    private String email;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;
}
