package com.winsalty.quickstart.auth.service.support;

import com.winsalty.quickstart.auth.config.RegisterMailProperties;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 基于 Spring Mail 的注册验证码发送实现。
 * SMTP 连接信息来自 spring.mail.*，实际部署时通过环境变量注入。
 * 创建日期：2026-04-19
 * author：sunshengxian
 */
@Service
public class JavaMailRegisterMailService implements RegisterMailService {

    private static final Logger log = LoggerFactory.getLogger(JavaMailRegisterMailService.class);

    private final JavaMailSender javaMailSender;
    private final RegisterMailProperties registerMailProperties;

    public JavaMailRegisterMailService(JavaMailSender javaMailSender,
                                       RegisterMailProperties registerMailProperties) {
        this.javaMailSender = javaMailSender;
        this.registerMailProperties = registerMailProperties;
    }

    @Override
    public void sendVerifyCode(String email, String code, long ttlSeconds) {
        if (!registerMailProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_SEND_FAILED, "邮箱验证码服务未启用");
        }
        if (!StringUtils.hasText(registerMailProperties.getFrom())) {
            throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_SEND_FAILED, "邮箱验证码发件人未配置");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(registerMailProperties.getFrom());
        message.setTo(email);
        message.setSubject(resolveSubject());
        message.setText(buildContent(code, ttlSeconds));
        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            log.error("register verify code mail send failed, email={}, message={}", email, exception.getMessage(), exception);
            throw new BusinessException(ErrorCode.REGISTER_VERIFY_CODE_SEND_FAILED, "邮箱验证码发送失败，请稍后重试");
        }
    }

    private String resolveSubject() {
        return StringUtils.hasText(registerMailProperties.getSubject())
                ? registerMailProperties.getSubject()
                : "Spring Admin 注册验证码";
    }

    private String buildContent(String code, long ttlSeconds) {
        long ttlMinutes = Math.max(1L, ttlSeconds / 60L);
        return "您好，\n\n"
                + "您正在注册 Spring Admin Starter，邮箱验证码为：\n\n"
                + code + "\n\n"
                + "验证码 " + ttlMinutes + " 分钟内有效，请勿泄露给他人。\n"
                + "如非本人操作，请忽略本邮件。";
    }
}
