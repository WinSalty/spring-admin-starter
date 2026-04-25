package com.winsalty.quickstart.infra.mail;

import com.aliyun.dm20151123.models.SingleSendMailRequest;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * 阿里云 DirectMail 邮件发送实现。
 * 在 app.mail.aliyun.enabled=true 时接管通用邮件发送，调用阿里云 SingleSendMail API 完成投递。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Service
@ConditionalOnProperty(prefix = "app.mail.aliyun", name = "enabled", havingValue = "true")
public class AliyunDirectMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(AliyunDirectMailService.class);
    private static final String DEFAULT_CLICK_TRACE = "0";

    private final MailProperties mailProperties;
    private final AliyunMailProperties aliyunMailProperties;
    private final AliyunDirectMailClient aliyunDirectMailClient;
    private final Executor mailTaskExecutor;

    public AliyunDirectMailService(MailProperties mailProperties,
                                   AliyunMailProperties aliyunMailProperties,
                                   AliyunDirectMailClient aliyunDirectMailClient,
                                   @Qualifier("mailTaskExecutor") Executor mailTaskExecutor) {
        this.mailProperties = mailProperties;
        this.aliyunMailProperties = aliyunMailProperties;
        this.aliyunDirectMailClient = aliyunDirectMailClient;
        this.mailTaskExecutor = mailTaskExecutor;
    }

    @Override
    public void send(MailSendRequest request) {
        MailSendSupport.validateRequest(mailProperties, request);
        validateAliyunConfig();
        boolean html = MailSendSupport.hasHtmlContent(request);
        String maskedTo = MailSendSupport.maskEmail(request.getTo());
        String subjectFingerprint = MailSendSupport.fingerprint(request.getSubject());
        try {
            // 阿里云请求对象先同步构建和校验，远程 API 调用放入线程池降低接口响应耗时。
            SingleSendMailRequest aliyunRequest = buildAliyunRequest(request);
            mailTaskExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    sendAliyunMail(aliyunRequest, maskedTo, subjectFingerprint, html);
                }
            });
            log.info("aliyun mail send task accepted, to={}, subjectFingerprint={}, html={}",
                    maskedTo, subjectFingerprint, html);
        } catch (MessagingException exception) {
            log.error("aliyun mail message build failed, to={}, subjectFingerprint={}",
                    maskedTo, subjectFingerprint, exception);
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED, "阿里云邮件内容构建失败");
        } catch (RejectedExecutionException exception) {
            log.error("aliyun mail send task rejected, to={}, subjectFingerprint={}",
                    maskedTo, subjectFingerprint, exception);
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED, "邮件发送队列繁忙，请稍后重试");
        }
    }

    private SingleSendMailRequest buildAliyunRequest(MailSendRequest request) throws MessagingException {
        InternetAddress toAddress = MailSendSupport.parseSingleAddress(request.getTo().trim(), "邮件收件人格式不正确");
        InternetAddress accountName = MailSendSupport.parseSingleAddress(resolveAccountName(), "阿里云邮件发信地址格式不正确");
        // SingleSendMail 一次只投递一个收件人，避免验证码类邮件出现批量收件人泄露。
        SingleSendMailRequest aliyunRequest = new SingleSendMailRequest()
                .setAccountName(accountName.getAddress())
                .setAddressType(aliyunMailProperties.getAddressType())
                .setReplyToAddress(aliyunMailProperties.isReplyToAddress())
                .setToAddress(toAddress.getAddress())
                .setSubject(request.getSubject().trim())
                .setClickTrace(resolveClickTrace());
        if (MailSendSupport.hasHtmlContent(request)) {
            aliyunRequest.setHtmlBody(request.getHtmlContent().trim());
        }
        if (MailSendSupport.hasTextContent(request)) {
            aliyunRequest.setTextBody(request.getTextContent().trim());
        }
        applyOptionalFields(aliyunRequest);
        return aliyunRequest;
    }

    private void applyOptionalFields(SingleSendMailRequest aliyunRequest) {
        // 可选字段仅在配置存在时写入，避免空字符串覆盖阿里云控制台默认设置。
        if (StringUtils.hasText(aliyunMailProperties.getFromAlias())) {
            aliyunRequest.setFromAlias(aliyunMailProperties.getFromAlias().trim());
        }
        if (StringUtils.hasText(aliyunMailProperties.getReplyAddress())) {
            aliyunRequest.setReplyAddress(aliyunMailProperties.getReplyAddress().trim());
        }
        if (StringUtils.hasText(aliyunMailProperties.getReplyAddressAlias())) {
            aliyunRequest.setReplyAddressAlias(aliyunMailProperties.getReplyAddressAlias().trim());
        }
        if (StringUtils.hasText(aliyunMailProperties.getTagName())) {
            aliyunRequest.setTagName(aliyunMailProperties.getTagName().trim());
        }
    }

    private void sendAliyunMail(SingleSendMailRequest request,
                                String maskedTo,
                                String subjectFingerprint,
                                boolean html) {
        try {
            aliyunDirectMailClient.send(request);
            log.info("aliyun mail sent success, to={}, subjectFingerprint={}, html={}",
                    maskedTo, subjectFingerprint, html);
        } catch (Exception exception) {
            log.error("aliyun mail send failed, to={}, subjectFingerprint={}",
                    maskedTo, subjectFingerprint, exception);
        }
    }

    private void validateAliyunConfig() {
        if (!aliyunMailProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED, "阿里云邮件服务未启用");
        }
        aliyunDirectMailClient.validateConfig();
        if (!StringUtils.hasText(resolveAccountName())) {
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED, "阿里云邮件发信地址未配置");
        }
    }

    private String resolveAccountName() {
        String accountName = MailSendSupport.safeTrim(aliyunMailProperties.getAccountName());
        return StringUtils.hasText(accountName) ? accountName : MailSendSupport.resolveFrom(mailProperties);
    }

    private String resolveClickTrace() {
        return StringUtils.hasText(aliyunMailProperties.getClickTrace())
                ? aliyunMailProperties.getClickTrace().trim()
                : DEFAULT_CLICK_TRACE;
    }
}
