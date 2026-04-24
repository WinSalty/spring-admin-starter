package com.winsalty.quickstart.auth.service.support;

/**
 * 注册邮箱验证服务接口。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
public interface RegisterVerificationService {

    void sendVerificationLink(String email, String verifyLinkBaseUrl);

    void verifyLink(String email, String token);
}
