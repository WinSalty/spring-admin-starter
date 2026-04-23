package com.winsalty.quickstart.infra.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 对象存储总开关配置。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.file.object-storage")
public class ObjectStorageProperties {

    /** 是否启用云对象存储；关闭时新文件写入本地存储，文件上传能力不关闭。 */
    private boolean enabled = false;
    /** 当前对象存储服务商。 */
    private String provider = "aliyun-oss";
    /** 本地存储配置。 */
    private Local local = new Local();

    /**
     * 本地文件存储配置。
     * 创建日期：2026-04-23
     * author：sunshengxian
     */
    @Data
    public static class Local {
        /** 本地文件根目录，生产环境应配置到持久化磁盘。 */
        private String rootPath = "uploads";
        /** 本地公共文件访问前缀。 */
        private String publicBaseUrl = "/api/file/public";
        /** 本地私有文件临时访问秒数，当前代理下载模式保留该配置。 */
        private long privateUrlExpireSeconds = 600L;
    }
}
