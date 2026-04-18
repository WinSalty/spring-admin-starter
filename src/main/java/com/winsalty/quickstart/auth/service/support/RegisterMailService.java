package com.winsalty.quickstart.auth.service.support;

/**
 * 注册验证码邮件发送服务。
 * 创建日期：2026-04-19
 * author：sunshengxian
 */
public interface RegisterMailService {

    void sendVerifyCode(String email, String code, long ttlSeconds);
}
