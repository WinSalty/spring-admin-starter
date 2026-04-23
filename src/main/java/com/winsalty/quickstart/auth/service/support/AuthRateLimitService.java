package com.winsalty.quickstart.auth.service.support;

/**
 * 认证限流服务。
 * 对登录、验证码发送等匿名入口做服务端限流，降低撞库、爆破和邮件轰炸风险。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
public interface AuthRateLimitService {

    /**
     * 校验登录请求限流。
     *
     * @param username 用户名
     * @param clientIp 客户端 IP
     */
    void checkLogin(String username, String clientIp);

    /**
     * 记录登录失败，用于账号锁定控制。
     *
     * @param username 用户名
     * @param clientIp 客户端 IP
     */
    void recordLoginFailure(String username, String clientIp);

    /**
     * 登录成功后清理失败计数和临时锁定。
     *
     * @param username 用户名
     * @param clientIp 客户端 IP
     */
    void recordLoginSuccess(String username, String clientIp);

    /**
     * 校验文件上传接口限流。
     *
     * @param username 当前登录用户名
     * @param clientIp 客户端 IP
     */
    void checkFileUpload(String username, String clientIp);

    /**
     * 校验注册验证码发送限流。
     *
     * @param email 邮箱
     * @param clientIp 客户端 IP
     */
    void checkRegisterVerifyCode(String email, String clientIp);
}
