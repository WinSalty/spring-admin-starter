package com.winsalty.quickstart.common.exception;

import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.log.dto.OperationLogRequest;
import com.winsalty.quickstart.log.service.LogService;
import com.winsalty.quickstart.common.util.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final LogService logService;

    public GlobalExceptionHandler(LogService logService) {
        this.logService = logService;
    }

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Object> handleBusinessException(BusinessException exception, HttpServletRequest request) {
        log.error("business exception, uri={}, message={}", request.getRequestURI(), exception.getMessage());
        recordExceptionLog(request.getRequestURI(), exception.getMessage(), "business", IpUtils.getClientIp(request));
        return ApiResponse.failure(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception,
                                                                     HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldError() == null
                ? "请求参数校验失败"
                : exception.getBindingResult().getFieldError().getDefaultMessage();
        log.error("method argument not valid, uri={}, message={}", request.getRequestURI(), message);
        return ApiResponse.failure(4001, message);
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<Object> handleBindException(BindException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldError() == null
                ? "请求参数绑定失败"
                : exception.getBindingResult().getFieldError().getDefaultMessage();
        log.error("bind exception, uri={}, message={}", request.getRequestURI(), message);
        return ApiResponse.failure(4002, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception,
                                                                     HttpServletRequest request) {
        log.error("request body unreadable, uri={}, message={}", request.getRequestURI(), exception.getMessage());
        return ApiResponse.failure(4003, "请求体格式错误");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Object> handleAccessDeniedException(AccessDeniedException exception, HttpServletRequest request) {
        log.error("access denied, uri={}, message={}", request.getRequestURI(), exception.getMessage());
        return ApiResponse.failure(4030, "无权限访问该资源");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> handleException(Exception exception, HttpServletRequest request) {
        log.error("system exception, uri={}, message={}", request.getRequestURI(), exception.getMessage(), exception);
        recordExceptionLog(request.getRequestURI(), exception.getMessage(), "system", IpUtils.getClientIp(request));
        return ApiResponse.failure(5000, "系统繁忙，请稍后重试");
    }

    private void recordExceptionLog(String target, String description, String logType, String ipAddress) {
        OperationLogRequest request = new OperationLogRequest();
        request.setLogType(logType);
        request.setOwner("system");
        request.setName("异常日志");
        request.setCode("exception_log");
        request.setDescription(description);
        request.setTarget(target);
        request.setIpAddress(ipAddress);
        request.setResult("失败");
        request.setDurationMs(0L);
        logService.record(request);
    }
}
