package com.winsalty.quickstart.auth.aop;

import com.winsalty.quickstart.auth.security.AuthContext;
import com.winsalty.quickstart.auth.security.AuthUser;
import com.winsalty.quickstart.auth.service.support.AuthRateLimitService;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.common.util.IpUtils;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 文件上传限流切面。
 * 拦截 @FileUploadRateLimit 标注的上传接口，避免 Controller 直接侵入限流调用。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Aspect
@Component
public class FileUploadRateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(FileUploadRateLimitAspect.class);

    private final AuthRateLimitService authRateLimitService;

    public FileUploadRateLimitAspect(AuthRateLimitService authRateLimitService) {
        this.authRateLimitService = authRateLimitService;
    }

    /**
     * 上传业务执行前统一限流，按当前登录用户和客户端 IP 两个维度控制频率。
     */
    @Before("@annotation(com.winsalty.quickstart.auth.annotation.FileUploadRateLimit)")
    public void beforeUpload() {
        HttpServletRequest request = currentRequest();
        AuthUser authUser = AuthContext.get();
        if (authUser == null) {
            throw new BusinessException(ErrorCode.AUTH_REQUIRED);
        }
        String clientIp = IpUtils.getClientIp(request);
        log.info("file upload rate limit check, username={}, clientIp={}", authUser.getUsername(), clientIp);
        authRateLimitService.checkFileUpload(authUser.getUsername(), clientIp);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException(ErrorCode.AUTH_REQUIRED);
        }
        return attributes.getRequest();
    }
}
