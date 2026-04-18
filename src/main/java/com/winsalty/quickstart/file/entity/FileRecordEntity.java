package com.winsalty.quickstart.file.entity;

/**
 * 文件记录实体。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
public class FileRecordEntity {
    /** 文件主键ID。 */
    private Long id;
    /** 文件记录编码。 */
    private String fileCode;
    /** 原始文件名。 */
    private String originalName;
    /** 存储文件名。 */
    private String storedName;
    /** 文件存储路径。 */
    private String filePath;
    /** 文件 MIME 类型。 */
    private String contentType;
    /** 文件扩展名。 */
    private String extension;
    /** 文件大小字节数。 */
    private Long sizeBytes;
    /** 文件状态。 */
    private String status;
    /** 逻辑删除标记。 */
    private Integer deleted;
    /** 创建人账号。 */
    private String createdBy;
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

    public String getFileCode() {
        return fileCode;
    }

    public void setFileCode(String fileCode) {
        this.fileCode = fileCode;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getStoredName() {
        return storedName;
    }

    public void setStoredName(String storedName) {
        this.storedName = storedName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
