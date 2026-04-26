package com.winsalty.quickstart.notice.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 公告通知保存请求。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Data
public class NoticeSaveRequest {

    private String id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过 100")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(max = 4000, message = "内容长度不能超过 4000")
    private String content;

    @NotBlank(message = "公告类型不能为空")
    @Size(max = 32, message = "公告类型长度不能超过 32")
    private String noticeType;

    @NotBlank(message = "优先级不能为空")
    @Pattern(regexp = "low|medium|normal|high|urgent", message = "优先级不合法")
    private String priority;

    private Boolean required = Boolean.FALSE;

    private Long publisherId;

    @Size(max = 19, message = "过期时间长度不能超过 19")
    private String expireTime;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "active|disabled|draft|expired", message = "状态值不合法")
    private String status;

    @Min(value = 0, message = "排序不能小于 0")
    private Integer sortOrder = 0;
}
