package com.winsalty.quickstart.notice.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 公告通知分页查询请求。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Data
public class NoticeListRequest {

    @Size(max = 64, message = "关键字长度不能超过 64")
    private String keyword;

    @Pattern(regexp = "active|disabled|draft|expired", message = "状态值不合法")
    private String status;

    @Size(max = 32, message = "公告类型长度不能超过 32")
    private String noticeType;

    @Min(value = 1, message = "pageNo 不能小于 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize 不能小于 1")
    @Max(value = 100, message = "pageSize 不能大于 100")
    private Integer pageSize = 10;
}
