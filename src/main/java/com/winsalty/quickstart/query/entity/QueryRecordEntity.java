package com.winsalty.quickstart.query.entity;

/**
 * 查询配置实体对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class QueryRecordEntity {

    /** 查询配置主键ID。 */
    private Long id;
    /** 查询记录编码。 */
    private String recordCode;
    /** 查询名称。 */
    private String name;
    /** 查询编码。 */
    private String code;
    /** 查询状态。 */
    private String status;
    /** 负责人。 */
    private String owner;
    /** 描述信息。 */
    private String description;
    /** 调用次数。 */
    private Long callCount;
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

    public String getRecordCode() {
        return recordCode;
    }

    public void setRecordCode(String recordCode) {
        this.recordCode = recordCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCallCount() {
        return callCount;
    }

    public void setCallCount(Long callCount) {
        this.callCount = callCount;
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
