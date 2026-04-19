package com.winsalty.quickstart.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 跨域配置。
 * 读取 app.cors.allowed-origins 白名单；未配置时不主动开放跨域。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:}")
    private List<String> allowedOrigins;

    /**
     * 允许前端开发地址携带 Authorization 头和 Cookie 类凭据访问后端。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            // 生产环境默认不开放跨域；只有显式配置白名单时才注册 CORS 规则。
            return;
        }
        registry.addMapping("/**")
                // 使用 allowedOrigins 而不是 "*"，否则 allowCredentials(true) 会被浏览器拒绝。
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
