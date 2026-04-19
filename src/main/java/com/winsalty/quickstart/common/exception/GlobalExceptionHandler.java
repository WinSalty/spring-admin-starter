package com.winsalty.quickstart.common.exception;

import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.constant.SystemConstants;
import com.winsalty.quickstart.common.util.IpUtils;
import com.winsalty.quickstart.log.dto.OperationLogRequest;
import com.winsalty.quickstart.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

/**
 * 全局异常处理器。
 * 负责把控制器抛出的异常统一收敛为 ApiResponse，保证前端错误处理稳定。
 * 业务异常和系统异常会额外写入异常日志，参数校验失败只返回给调用方。
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

    /**
     * 处理主动抛出的业务异常，保留业务码并记录异常日志。
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Object> handleBusinessException(BusinessException exception, HttpServletRequest request) {
        log.error("business exception, uri={}, message={}", request.getRequestURI(), exception.getMessage());
        // 业务异常代表可预期失败，响应保留原业务码；同时写异常日志，方便后台追踪高频失败原因。
        recordExceptionLog(request.getRequestURI(), exception.getMessage(), "business", IpUtils.getClientIp(request));
        return ApiResponse.failure(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception,
                                                                     HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldError() == null
                ? "请求参数校验失败"
                : exception.getBindingResult().getFieldError().getDefaultMessage();
        // JSON body 的字段校验错误直接返回首个字段提示，避免前端再解析复杂 BindingResult。
        log.error("method argument not valid, uri={}, message={}", request.getRequestURI(), message);
        return ApiResponse.failure(ErrorCode.REQUEST_PARAM_INVALID.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<Object> handleBindException(BindException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldError() == null
                ? "请求参数绑定失败"
                : exception.getBindingResult().getFieldError().getDefaultMessage();
        log.error("bind exception, uri={}, message={}", request.getRequestURI(), message);
        return ApiResponse.failure(ErrorCode.REQUEST_BIND_INVALID.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Object> handleConstraintViolationException(ConstraintViolationException exception,
                                                                  HttpServletRequest request) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("请求参数校验失败");
        // @RequestParam/@PathVariable 的校验异常不经过 BindingResult，需要单独收敛。
        log.error("constraint violation, uri={}, message={}", request.getRequestURI(), message);
        return ApiResponse.failure(ErrorCode.REQUEST_PARAM_INVALID.getCode(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception,
                                                                     HttpServletRequest request) {
        log.error("request body unreadable, uri={}, message={}", request.getRequestURI(), exception.getMessage());
        return ApiResponse.failure(ErrorCode.REQUEST_BODY_INVALID.getCode(), ErrorCode.REQUEST_BODY_INVALID.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Object> handleAccessDeniedException(AccessDeniedException exception, HttpServletRequest request) {
        log.error("access denied, uri={}, message={}", request.getRequestURI(), exception.getMessage());
        return ApiResponse.failure(ErrorCode.ACCESS_DENIED.getCode(), ErrorCode.ACCESS_DENIED.getMessage());
    }

    /**
     * 兜底处理未预期异常。对外只返回通用提示，详细堆栈保留在服务端日志。
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> handleException(Exception exception, HttpServletRequest request) {
        log.error("system exception, uri={}, message={}", request.getRequestURI(), exception.getMessage(), exception);
        // 未预期异常不把堆栈或数据库错误暴露给前端，只在服务端日志和异常日志保留细节。
        recordExceptionLog(request.getRequestURI(), exception.getMessage(), "system", IpUtils.getClientIp(request));
        return ApiResponse.failure(5000, "系统繁忙，请稍后重试");
    }

    /**
     * 将异常事件写入系统日志模块。日志写入失败由 LogService 内部吞吐，不反向影响接口响应。
     */
    private void recordExceptionLog(String target, String description, String logType, String ipAddress) {
        OperationLogRequest request = new OperationLogRequest();
        // 这里复用日志模块的 DTO，统一进入 operation_log 表；LogService 内部会吞掉二次写日志失败。
        request.setLogType(logType);
        request.setOwner(SystemConstants.SYSTEM_OPERATOR);
        request.setName("异常日志");
        request.setCode("exception_log");
        request.setDescription(description);
        request.setTarget(target);
        request.setIpAddress(ipAddress);
        request.setResult(SystemConstants.RESULT_FAILURE);
        request.setDurationMs(0L);
        logService.record(request);
    }
}
