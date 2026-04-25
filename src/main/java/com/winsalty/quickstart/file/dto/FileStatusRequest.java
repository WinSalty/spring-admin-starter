package com.winsalty.quickstart.file.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 文件状态更新请求。
 * 承载目标启停状态。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
@Data
public class FileStatusRequest {
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;
}
