package com.winsalty.quickstart.department.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 部门状态更新请求。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Data
public class DepartmentStatusRequest {

    @NotBlank(message = "id 不能为空")
    private String id;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;
}
