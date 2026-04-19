package com.winsalty.quickstart.system.dto;

import lombok.Data;

import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;

/**
 * 菜单树查询请求对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class SystemMenuListRequest {

    @Size(max = 120, message = "关键字长度不能超过 120")
    private String keyword;

    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;
}
