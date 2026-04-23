package com.winsalty.quickstart.file.vo;

import lombok.Data;

/**
 * 文件记录响应对象。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Data
public class FileRecordVo {
    private String id;
    private String originalName;
    private String storedName;
    private String storageType;
    private String objectKey;
    private String fileUrl;
    private String contentHash;
    private String contentType;
    private String extension;
    private Long sizeBytes;
    private String status;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
}
