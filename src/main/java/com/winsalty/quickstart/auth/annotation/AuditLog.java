package com.winsalty.quickstart.auth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解。
 * 标注在需要落操作日志的方法上，由 AuditLogAspect 统一采集请求、响应和耗时。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * 日志分类，例如 login、operation、api、business。
     */
    String logType() default "operation";

    /**
     * 稳定的操作编码，用于前端筛选和后续统计。
     */
    String code();

    /**
     * 用户可读的操作名称。
     */
    String name();

    /**
     * 操作目标；为空时默认使用当前请求 URI。
     */
    String target() default "";

    /**
     * 是否记录请求摘要。涉及大文件或敏感数据的接口可以关闭。
     */
    boolean recordRequest() default true;

    /**
     * 是否记录响应摘要。返回大对象或下载流的接口可以关闭。
     */
    boolean recordResponse() default true;
}
