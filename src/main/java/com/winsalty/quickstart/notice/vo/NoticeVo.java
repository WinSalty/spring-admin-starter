package com.winsalty.quickstart.notice.vo;

import lombok.Data;

/**
 * 公告通知响应对象。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Data
public class NoticeVo {

    private String id;
    private String title;
    private String content;
    private String noticeType;
    private String priority;
    private Boolean required;
    private String publisherId;
    private String publisherName;
    private String publishTime;
    private String expireTime;
    private String status;
    private Integer sortOrder;
    private String createdAt;
    private String updatedAt;
}
