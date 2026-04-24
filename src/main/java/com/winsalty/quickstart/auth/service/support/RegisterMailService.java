package com.winsalty.quickstart.auth.service.support;

/**
 * 注册账号激活邮件发送服务。
 * 复用通用邮件服务，封装注册激活业务的邮件模板。
 * 创建日期：2026-04-19
 * author：sunshengxian
 */
public interface RegisterMailService {

    boolean isEnabled();

    void sendVerificationLink(String email, String verificationUrl, long ttlSeconds);
}
