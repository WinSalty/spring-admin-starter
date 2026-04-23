package com.winsalty.quickstart.infra.mail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 通用邮件服务配置。
 * 管理邮件服务总开关、默认发件人、默认编码和统一模板样式。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    /**
     * 是否启用通用邮件服务；关闭后所有业务邮件统一失败。
     */
    private boolean enabled = true;

    /**
     * 默认发件人，未传业务级 from 时使用。
     */
    private String from;

    /**
     * 默认编码，和 spring.mail.default-encoding 保持一致。
     */
    private String defaultEncoding = "UTF-8";

    /**
     * 邮件模板统一配置。
     */
    private Template template = new Template();

    /**
     * 邮件模板样式配置。
     * 创建日期：2026-04-23
     * author：sunshengxian
     */
    @Data
    public static class Template {

        /**
         * 品牌展示名称。
         */
        private String brandName = "React Admin Starter";

        /**
         * 模板页脚签名。
         */
        private String signature = "React Admin Starter";

        /**
         * 主色调。
         */
        private String primaryColor = "#1677ff";

        /**
         * 页面背景色。
         */
        private String backgroundColor = "#f4f7fb";

        /**
         * 卡片背景色。
         */
        private String cardBackgroundColor = "#ffffff";
    }
}
