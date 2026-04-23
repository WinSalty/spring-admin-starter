package com.winsalty.quickstart.auth.service.support;

/**
 * 注册验证码邮件发送服务。
 * 复用通用邮件服务，封装注册业务的邮件模板。
 * 创建日期：2026-04-19
 * author：sunshengxian
 */
public interface RegisterMailService {

    boolean isEnabled();

    void sendVerifyCode(String email, String code, long ttlSeconds);
}
