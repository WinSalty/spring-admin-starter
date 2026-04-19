package com.winsalty.quickstart.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 系统配置保存请求对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class SystemConfigSaveRequest {

    @NotBlank(message = "id 不能为空")
    private String id;

    @NotNull(message = "value 不能为空")
    private Object value;
}
