package com.winsalty.quickstart.infra.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 对象存储配置。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.file.object-storage.aliyun")
public class AliyunOssStorageProperties {

    /** 阿里云 OSS Endpoint，例如 https://oss-cn-hangzhou.aliyuncs.com。 */
    private String endpoint;
    /** 阿里云 AccessKeyId。 */
    private String accessKeyId;
    /** 阿里云 AccessKeySecret。 */
    private String accessKeySecret;
    /** Bucket 名称。 */
    private String bucket;
    /** 外链域名，例如 https://static.example.com。 */
    private String domain;
    /** 对象 key 前缀。 */
    private String keyPrefix = "uploads";
}
