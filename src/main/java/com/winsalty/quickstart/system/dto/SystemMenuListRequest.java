package com.winsalty.quickstart.system.dto;

import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;

/**
 * 菜单树查询请求对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class SystemMenuListRequest {

    @Size(max = 120, message = "关键字长度不能超过 120")
    private String keyword;

    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
