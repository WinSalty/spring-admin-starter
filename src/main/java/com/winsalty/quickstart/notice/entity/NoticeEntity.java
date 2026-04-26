package com.winsalty.quickstart.notice.entity;

import lombok.Data;

/**
 * 公告通知实体。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Data
public class NoticeEntity {

    /** 公告主键ID。 */
    private Long id;
    /** 公告标题。 */
    private String title;
    /** 公告内容。 */
    private String content;
    /** 公告类型。 */
    private String noticeType;
    /** 公告优先级。 */
    private String priority;
    /** 是否必读公告。 */
    private Boolean required;
    /** 发布人用户ID。 */
    private Long publisherId;
    /** 发布人名称。 */
    private String publisherName;
    /** 发布时间。 */
    private String publishTime;
    /** 过期时间。 */
    private String expireTime;
    /** 公告状态。 */
    private String status;
    /** 排序号。 */
    private Integer sortOrder;
    /** 逻辑删除标记。 */
    private Integer deleted;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
