package com.winsalty.quickstart.infra.storage;

import lombok.Data;

/**
 * 对象存储上传结果。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Data
public class ObjectStorageUploadResult {

    /** 存储类型，例如 local 或 aliyun-oss。 */
    private String storageType;
    /** Bucket 类型，例如 public、private、temp。 */
    private String bucketType;
    /** 实际 Bucket 名称或本地逻辑空间。 */
    private String bucketName;
    /** 访问策略，例如 public_read、private_read。 */
    private String accessPolicy;
    /** 对象存储 key。 */
    private String objectKey;
    /** 可访问文件地址。 */
    private String fileUrl;
    /** 服务端保存路径或对象 key。 */
    private String filePath;
}
