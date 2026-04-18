package com.winsalty.quickstart.auth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    String logType() default "operation";

    String code();

    String name();

    String target() default "";

    boolean recordRequest() default true;

    boolean recordResponse() default true;
}
