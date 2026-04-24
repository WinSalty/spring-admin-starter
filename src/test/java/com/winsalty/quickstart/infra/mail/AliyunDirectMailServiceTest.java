package com.winsalty.quickstart.infra.mail;

import com.aliyun.dm20151123.models.SingleSendMailRequest;
import com.winsalty.quickstart.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 阿里云邮件发送服务测试。
 * 覆盖 DirectMail 请求字段映射、配置缺失拒绝和业务发件人覆盖防护。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
class AliyunDirectMailServiceTest {

    private static final String ACCOUNT_NAME = "notice@example.com";
    private static final String TO_EMAIL = "test@example.com";
    private static final String SUBJECT = "注册验证码";
    private static final String TEXT_CONTENT = "验证码 123456";
    private static final String HTML_CONTENT = "<strong>验证码 123456</strong>";
    private static final String FROM_ALIAS = "Spring Admin";
    private static final String TAG_NAME = "register";

    @Test
    void sendMapsRequestToAliyunSingleSendMail() throws Exception {
        AliyunDirectMailClient aliyunDirectMailClient = mock(AliyunDirectMailClient.class);
        AliyunDirectMailService service = new AliyunDirectMailService(
                enabledMailProperties(), enabledAliyunProperties(), aliyunDirectMailClient, Runnable::run);

        service.send(MailSendRequest.html(TO_EMAIL, SUBJECT, TEXT_CONTENT, HTML_CONTENT));

        ArgumentCaptor<SingleSendMailRequest> requestCaptor = ArgumentCaptor.forClass(SingleSendMailRequest.class);
        verify(aliyunDirectMailClient).send(requestCaptor.capture());
        SingleSendMailRequest request = requestCaptor.getValue();
        assertEquals(ACCOUNT_NAME, request.getAccountName());
        assertEquals(TO_EMAIL, request.getToAddress());
        assertEquals(SUBJECT, request.getSubject());
        assertEquals(TEXT_CONTENT, request.getTextBody());
        assertEquals(HTML_CONTENT, request.getHtmlBody());
        assertEquals(Integer.valueOf(1), request.getAddressType());
        assertEquals(Boolean.TRUE, request.getReplyToAddress());
        assertEquals(FROM_ALIAS, request.getFromAlias());
        assertEquals(TAG_NAME, request.getTagName());
        assertEquals("0", request.getClickTrace());
    }

    @Test
    void sendRejectsWhenAliyunAccountNameMissing() throws Exception {
        AliyunDirectMailClient aliyunDirectMailClient = mock(AliyunDirectMailClient.class);
        MailProperties mailProperties = enabledMailProperties();
        mailProperties.setFrom(null);
        AliyunMailProperties aliyunMailProperties = enabledAliyunProperties();
        aliyunMailProperties.setAccountName(null);
        AliyunDirectMailService service = new AliyunDirectMailService(
                mailProperties, aliyunMailProperties, aliyunDirectMailClient, Runnable::run);

        assertThrows(BusinessException.class, () -> service.send(MailSendRequest.text(TO_EMAIL, SUBJECT, TEXT_CONTENT)));

        verify(aliyunDirectMailClient, never()).send(any(SingleSendMailRequest.class));
    }

    @Test
    void sendRejectsBusinessFromOverride() throws Exception {
        AliyunDirectMailClient aliyunDirectMailClient = mock(AliyunDirectMailClient.class);
        AliyunDirectMailService service = new AliyunDirectMailService(
                enabledMailProperties(), enabledAliyunProperties(), aliyunDirectMailClient, Runnable::run);
        MailSendRequest request = MailSendRequest.text(TO_EMAIL, SUBJECT, TEXT_CONTENT);
        request.setFrom("spoof@example.com");

        assertThrows(BusinessException.class, () -> service.send(request));

        verify(aliyunDirectMailClient, never()).send(any(SingleSendMailRequest.class));
    }

    private MailProperties enabledMailProperties() {
        MailProperties properties = new MailProperties();
        properties.setEnabled(true);
        properties.setFrom(ACCOUNT_NAME);
        return properties;
    }

    private AliyunMailProperties enabledAliyunProperties() {
        AliyunMailProperties properties = new AliyunMailProperties();
        properties.setEnabled(true);
        properties.setEndpoint("dm.aliyuncs.com");
        properties.setRegionId("cn-hangzhou");
        properties.setAccessKeyId("access-key-id");
        properties.setAccessKeySecret("access-key-secret");
        properties.setAccountName(ACCOUNT_NAME);
        properties.setFromAlias(FROM_ALIAS);
        properties.setTagName(TAG_NAME);
        return properties;
    }
}
