package com.winsalty.quickstart.file.vo;

import lombok.Data;

@Data
public class FileRecordVo {
    private String id;
    private String originalName;
    private String storedName;
    private String contentType;
    private String extension;
    private Long sizeBytes;
    private String status;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
}
