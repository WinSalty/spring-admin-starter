package com.winsalty.quickstart.infra.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 七牛云对象存储配置。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.file.qiniu")
public class QiniuStorageProperties {

    /** 七牛云 AccessKey。 */
    private String accessKey;
    /** 七牛云 SecretKey。 */
    private String secretKey;
    /** 空间名称。 */
    private String bucket;
    /** 外链域名，例如 https://static.example.com。 */
    private String domain;
    /** 存储区域，支持 auto、huadong、huabei、huanan、beimei、xinjiapo。 */
    private String region = "auto";
    /** 对象 key 前缀。 */
    private String keyPrefix = "uploads";
}
