package com.winsalty.quickstart.system.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 系统管理状态更新请求对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class SystemStatusRequest {

    @NotBlank(message = "id 不能为空")
    private String id;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
