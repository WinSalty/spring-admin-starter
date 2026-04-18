package com.winsalty.quickstart.notice.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 公告通知保存请求。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
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
    @Pattern(regexp = "low|normal|high|urgent", message = "优先级不合法")
    private String priority;

    private Long publisherId;

    @Size(max = 19, message = "发布时间长度不能超过 19")
    private String publishTime;

    @Size(max = 19, message = "过期时间长度不能超过 19")
    private String expireTime;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "active|disabled|draft|expired", message = "状态值不合法")
    private String status;

    @Min(value = 0, message = "排序不能小于 0")
    private Integer sortOrder = 0;

    public String getId() {
        return id;
    }

    public void setId(String id) {
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
}
