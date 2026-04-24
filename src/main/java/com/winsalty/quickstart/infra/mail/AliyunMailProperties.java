package com.winsalty.quickstart.infra.mail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云邮件推送配置。
 * 管理 DirectMail API 发信地址、访问密钥、区域、超时和邮件跟踪参数。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.mail.aliyun")
public class AliyunMailProperties {

    /** 是否启用阿里云邮件推送；关闭时默认使用 SMTP 发送。 */
    private boolean enabled = false;
    /** 阿里云 DirectMail Endpoint。 */
    private String endpoint = "dm.aliyuncs.com";
    /** 阿里云区域。 */
    private String regionId = "cn-hangzhou";
    /** 阿里云 AccessKeyId。 */
    private String accessKeyId;
    /** 阿里云 AccessKeySecret。 */
    private String accessKeySecret;
    /** 邮件推送控制台配置的发信地址。 */
    private String accountName;
    /** 发信人昵称。 */
    private String fromAlias;
    /** 地址类型，1 表示发信地址。 */
    private int addressType = 1;
    /** 是否启用控制台配置的回信地址。 */
    private boolean replyToAddress = true;
    /** 回信地址。 */
    private String replyAddress;
    /** 回信地址昵称。 */
    private String replyAddressAlias;
    /** 邮件标签。 */
    private String tagName;
    /** 点击跟踪开关，0 关闭，1 开启。 */
    private String clickTrace = "0";
    /** SDK 建连超时时间，单位毫秒。 */
    private int connectTimeout = 5000;
    /** SDK 读取超时时间，单位毫秒。 */
    private int readTimeout = 10000;
}
