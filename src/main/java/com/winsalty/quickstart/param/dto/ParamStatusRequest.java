package com.winsalty.quickstart.param.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 参数配置状态更新请求。
 * 承载参数 ID 和目标启停状态。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
@Data
public class ParamStatusRequest {
    @NotBlank(message = "id 不能为空")
    private String id;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;
}
