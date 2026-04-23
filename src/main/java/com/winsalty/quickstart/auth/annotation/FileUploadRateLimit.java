package com.winsalty.quickstart.auth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 文件上传限流注解。
 * 标注在文件上传入口方法上，由切面统一按用户和 IP 执行限流校验。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FileUploadRateLimit {
}
