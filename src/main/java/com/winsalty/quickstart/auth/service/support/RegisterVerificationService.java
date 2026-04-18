package com.winsalty.quickstart.auth.service.support;

/**
 * 注册验证码服务接口。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
public interface RegisterVerificationService {

    String generateCode(String email);

    void verifyCode(String email, String code);
}
