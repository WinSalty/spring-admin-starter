package com.winsalty.quickstart.file.entity;

import lombok.Data;

/**
 * 文件记录实体。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Data
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
    /** 存储类型。 */
    private String storageType;
    /** 对象存储 key。 */
    private String objectKey;
    /** 文件访问地址。 */
    private String fileUrl;
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
}
