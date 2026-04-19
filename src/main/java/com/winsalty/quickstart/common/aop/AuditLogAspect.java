package com.winsalty.quickstart.common.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winsalty.quickstart.auth.annotation.AuditLog;
import com.winsalty.quickstart.auth.security.AuthContext;
import com.winsalty.quickstart.auth.security.AuthUser;
import com.winsalty.quickstart.common.constant.CommonStatusConstants;
import com.winsalty.quickstart.common.constant.SystemConstants;
import com.winsalty.quickstart.common.util.IpUtils;
import com.winsalty.quickstart.log.dto.OperationLogRequest;
import com.winsalty.quickstart.log.service.LogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审计日志切面。
 * 拦截标注 @AuditLog 的业务方法，在方法成功或失败后记录操作日志。
 * 该切面只负责采集上下文和脱敏摘要，不参与业务事务决策。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    private final LogService logService;
    private final ObjectMapper objectMapper;

    public AuditLogAspect(LogService logService, ObjectMapper objectMapper) {
        this.logService = logService;
        this.objectMapper = objectMapper;
    }

    /**
     * Around 通知包住业务方法，确保成功和异常分支都会尝试落审计日志。
     */
    @Around("@annotation(com.winsalty.quickstart.auth.annotation.AuditLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startedAt = System.currentTimeMillis();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        AuditLog auditLog = method.getAnnotation(AuditLog.class);
        HttpServletRequest request = currentRequest();
        try {
            Object result = joinPoint.proceed();
            // 成功分支在业务方法返回后落日志，保证响应摘要能记录到最终结果。
            record(joinPoint, auditLog, request, result, null, startedAt);
            return result;
        } catch (Throwable throwable) {
            // 异常分支先记录失败日志再继续抛出，让全局异常处理器仍然负责 HTTP 响应。
            record(joinPoint, auditLog, request, null, throwable, startedAt);
            throw throwable;
        }
    }

    /**
     * 构造日志 DTO 并写入日志服务。请求、响应是否入库由注解参数控制。
     */
    private void record(ProceedingJoinPoint joinPoint,
                        AuditLog auditLog,
                        HttpServletRequest request,
                        Object result,
                        Throwable throwable,
                        long startedAt) {
        OperationLogRequest logRequest = new OperationLogRequest();
        // 注解描述“这是什么操作”，请求上下文描述“谁在什么入口触发”，两者组合成完整审计记录。
        logRequest.setLogType(auditLog.logType());
        logRequest.setOwner(resolveOwner());
        logRequest.setName(auditLog.name());
        logRequest.setCode(auditLog.code());
        logRequest.setTarget(resolveTarget(auditLog, request));
        logRequest.setIpAddress(IpUtils.getClientIp(request));
        logRequest.setDeviceInfo(resolveDeviceInfo(request));
        logRequest.setRequestInfo(auditLog.recordRequest() ? safeJson(buildRequestPayload(joinPoint, request)) : "");
        logRequest.setResponseInfo(auditLog.recordResponse() ? safeJson(buildResponsePayload(result, throwable)) : "");
        // result 只表示业务方法是否抛异常，不依赖 ApiResponse.code，避免切面对响应协议产生强耦合。
        logRequest.setResult(throwable == null ? SystemConstants.RESULT_SUCCESS : SystemConstants.RESULT_FAILURE);
        logRequest.setDurationMs(System.currentTimeMillis() - startedAt);
        logRequest.setDescription(buildDescription(joinPoint, request, throwable));
        logService.record(logRequest);
    }

    private String resolveOwner() {
        AuthUser authUser = AuthContext.get();
        return authUser == null ? SystemConstants.SYSTEM_OPERATOR : authUser.getUsername();
    }

    private String resolveTarget(AuditLog auditLog, HttpServletRequest request) {
        if (StringUtils.hasText(auditLog.target())) {
            return auditLog.target();
        }
        return request == null ? "" : request.getRequestURI();
    }

    private String resolveDeviceInfo(HttpServletRequest request) {
        return request == null ? "" : defaultText(request.getHeader("User-Agent"));
    }

    private Map<String, Object> buildRequestPayload(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        // 只记录定位问题需要的最小请求信息，header/body 原文不入库，降低敏感信息暴露面。
        payload.put("method", request == null ? "" : request.getMethod());
        payload.put("uri", request == null ? "" : request.getRequestURI());
        payload.put("args", sanitize(joinPoint.getArgs()));
        return payload;
    }

    private Map<String, Object> buildResponsePayload(Object result, Throwable throwable) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        if (throwable != null) {
            payload.put("error", throwable.getMessage());
        } else {
            payload.put("result", sanitize(result));
        }
        return payload;
    }

    /**
     * 对审计内容做最低限度脱敏，避免密码、token、验证码进入操作日志。
     */
    private Object sanitize(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        // DTO 通常会被序列化为字符串，这里用字段名匹配做最后一道脱敏防线。
        text = text.replaceAll("(?i)\\\"password\\\"\\s*:\\s*\\\"[^\\\"]*\\\"", "\"password\":\"***\"");
        text = text.replaceAll("(?i)\\\"token\\\"\\s*:\\s*\\\"[^\\\"]*\\\"", "\"token\":\"***\"");
        text = text.replaceAll("(?i)\\\"refreshToken\\\"\\s*:\\s*\\\"[^\\\"]*\\\"", "\"refreshToken\":\"***\"");
        text = text.replaceAll("(?i)\\\"verifyCode\\\"\\s*:\\s*\\\"[^\\\"]*\\\"", "\"verifyCode\":\"***\"");
        return text;
    }

    private String buildDescription(ProceedingJoinPoint joinPoint, HttpServletRequest request, Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        builder.append(joinPoint.getSignature().getDeclaringTypeName()).append("#").append(joinPoint.getSignature().getName());
        if (request != null) {
            builder.append(" ").append(request.getMethod()).append(" ").append(request.getRequestURI());
        }
        if (throwable != null && StringUtils.hasText(throwable.getMessage())) {
            builder.append(" -> ").append(throwable.getMessage());
        }
        return builder.toString();
    }

    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            log.error("audit log serialize failed, message={}", exception.getMessage());
            return "";
        }
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // AOP 也可能包到非 Web 调用，返回 null 后各字段按空字符串处理。
        return attributes == null ? null : attributes.getRequest();
    }
}
