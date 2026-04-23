package com.winsalty.quickstart.file.vo;

import lombok.Data;

/**
 * 对象存储状态响应对象。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Data
public class ObjectStorageStatusVo {

    /** 是否启用对象存储服务。 */
    private Boolean enabled;
    /** 对象存储服务商。 */
    private String provider;
    /** 文件上传能力是否启用。 */
    private Boolean fileUploadEnabled;
    /** 当前新文件写入存储类型。 */
    private String activeStorageType;
}
