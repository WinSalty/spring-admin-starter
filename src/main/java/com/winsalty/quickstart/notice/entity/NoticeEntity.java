package com.winsalty.quickstart.notice.entity;

/**
 * 公告通知实体。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getNoticeType() {
        return noticeType;
    }

    public void setNoticeType(String noticeType) {
        this.noticeType = noticeType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Long getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    public String getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(String publishTime) {
        this.publishTime = publishTime;
    }

    public String getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(String expireTime) {
        this.expireTime = expireTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
