package com.winsalty.quickstart.notice.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 公告通知状态更新请求。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Data
public class NoticeStatusRequest {

    @NotBlank(message = "id 不能为空")
    private String id;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "active|disabled|draft|expired", message = "状态值不合法")
    private String status;
}
