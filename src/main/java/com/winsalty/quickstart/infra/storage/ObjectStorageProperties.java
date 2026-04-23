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

    /** 是否启用对象存储；关闭时普通文件使用本地存储，头像不落盘并使用用户名首字展示。 */
    private boolean enabled = false;
    /** 当前对象存储服务商。 */
    private String provider = "aliyun-oss";
}
