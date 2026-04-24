package com.winsalty.quickstart.infra.mail;

import com.aliyun.dm20151123.Client;
import com.aliyun.dm20151123.models.SingleSendMailRequest;
import com.aliyun.teaopenapi.models.Config;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 阿里云 DirectMail API 客户端封装。
 * 负责 SDK Client 懒加载、基础配置校验和单封邮件 API 调用。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Component
@ConditionalOnProperty(prefix = "app.mail.aliyun", name = "enabled", havingValue = "true")
public class AliyunDirectMailClient {

    private static final String HTTPS_PROTOCOL = "HTTPS";

    private final AliyunMailProperties properties;
    private volatile Client client;

    public AliyunDirectMailClient(AliyunMailProperties properties) {
        this.properties = properties;
    }

    /**
     * 调用阿里云 SingleSendMail API 发送单封邮件。
     *
     * @param request 单封邮件请求
     * @throws Exception SDK 调用异常
     * @author sunshengxian
     * @date 2026-04-24
     */
    public void send(SingleSendMailRequest request) throws Exception {
        getClient().singleSendMail(request);
    }

    /**
     * 校验阿里云邮件推送基础配置。
     *
     * @author sunshengxian
     * @date 2026-04-24
     */
    public void validateConfig() {
        if (!StringUtils.hasText(properties.getEndpoint())
                || !StringUtils.hasText(properties.getRegionId())
                || !StringUtils.hasText(properties.getAccessKeyId())
                || !StringUtils.hasText(properties.getAccessKeySecret())) {
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED, "阿里云邮件服务配置不完整");
        }
    }

    private Client getClient() throws Exception {
        if (client != null) {
            return client;
        }
        synchronized (this) {
            if (client == null) {
                validateConfig();
                Config config = new Config()
                        .setAccessKeyId(properties.getAccessKeyId())
                        .setAccessKeySecret(properties.getAccessKeySecret())
                        .setEndpoint(properties.getEndpoint())
                        .setRegionId(properties.getRegionId())
                        .setProtocol(HTTPS_PROTOCOL)
                        .setConnectTimeout(properties.getConnectTimeout())
                        .setReadTimeout(properties.getReadTimeout());
                client = new Client(config);
            }
        }
        return client;
    }
}
