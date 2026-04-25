package com.winsalty.quickstart.system.dict.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 字典状态更新请求。
 * 承载字典类型或字典项 ID 和目标启停状态。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
@Data
public class DictStatusRequest {
    @NotBlank(message = "id 不能为空")
    private String id;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;
}
